#include <jni.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES3/gl3.h>
#include <pthread.h>
#include <unistd.h>
#include <dlfcn.h>
#include <string.h>
#include <vector>
#include <algorithm>
#include <mutex>

#include "pl/Hook.h"
#include "pl/Gloss.h"

const char* vertexShaderSource = R"(
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
)";

const char* blendFragmentShaderSource = R"(
precision highp float;
varying vec2 vTexCoord;
uniform sampler2D uCurrentFrame;
uniform sampler2D uHistoryFrame;
uniform float uBlendFactor;
void main() {
    vec4 current = texture2D(uCurrentFrame, vTexCoord);
    vec4 history = texture2D(uHistoryFrame, vTexCoord);
    vec4 result = mix(current, history, uBlendFactor);
    gl_FragColor = vec4(result.rgb, 1.0);
}
)";

const char* drawFragmentShaderSource = R"(
precision highp float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;
void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    gl_FragColor = vec4(color.rgb, 1.0);
}
)";

static bool motion_blur_enabled = false;
static float blur_strength = 0.85f;

static GLuint rawTexture = 0;
static GLuint historyTextures[2] = {0, 0};
static GLuint historyFBOs[2] = {0, 0};
static int pingPongIndex = 0;
static bool isFirstFrame = true;

static GLuint blendShaderProgram = 0;
static GLint blendPosLoc = -1;
static GLint blendTexCoordLoc = -1;
static GLint blendCurrentLoc = -1;
static GLint blendHistoryLoc = -1;
static GLint blendFactorLoc = -1;

static GLuint drawShaderProgram = 0;
static GLint drawPosLoc = -1;
static GLint drawTexCoordLoc = -1;
static GLint drawTextureLoc = -1;

static GLuint vertexBuffer = 0;
static GLuint indexBuffer = 0;

static int blur_res_width = 0;
static int blur_res_height = 0;

void initializeMotionBlurResources(GLint width, GLint height) {
    if (rawTexture != 0) {
        glDeleteTextures(1, &rawTexture);
        glDeleteTextures(2, historyTextures);
        glDeleteFramebuffers(2, historyFBOs);
    }

    if (blendShaderProgram == 0) {
        GLuint vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, 1, &vertexShaderSource, nullptr);
        glCompileShader(vs);

        GLuint fsBlend = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsBlend, 1, &blendFragmentShaderSource, nullptr);
        glCompileShader(fsBlend);

        blendShaderProgram = glCreateProgram();
        glAttachShader(blendShaderProgram, vs);
        glAttachShader(blendShaderProgram, fsBlend);
        glLinkProgram(blendShaderProgram);

        blendPosLoc = glGetAttribLocation(blendShaderProgram, "aPosition");
        blendTexCoordLoc = glGetAttribLocation(blendShaderProgram, "aTexCoord");
        blendCurrentLoc = glGetUniformLocation(blendShaderProgram, "uCurrentFrame");
        blendHistoryLoc = glGetUniformLocation(blendShaderProgram, "uHistoryFrame");
        blendFactorLoc = glGetUniformLocation(blendShaderProgram, "uBlendFactor");

        GLuint fsDraw = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fsDraw, 1, &drawFragmentShaderSource, nullptr);
        glCompileShader(fsDraw);

        drawShaderProgram = glCreateProgram();
        glAttachShader(drawShaderProgram, vs);
        glAttachShader(drawShaderProgram, fsDraw);
        glLinkProgram(drawShaderProgram);

        drawPosLoc = glGetAttribLocation(drawShaderProgram, "aPosition");
        drawTexCoordLoc = glGetAttribLocation(drawShaderProgram, "aTexCoord");
        drawTextureLoc = glGetUniformLocation(drawShaderProgram, "uTexture");

        GLfloat vertices[] = {
            -1.0f, 1.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 1.0f
        };
        GLushort indices[] = { 0, 1, 2, 0, 2, 3 };

        glGenBuffers(1, &vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);

        glGenBuffers(1, &indexBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices, GL_STATIC_DRAW);
    }

    glGenTextures(1, &rawTexture);
    glBindTexture(GL_TEXTURE_2D, rawTexture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glGenTextures(2, historyTextures);
    glGenFramebuffers(2, historyFBOs);

    for (int i = 0; i < 2; i++) {
        glBindTexture(GL_TEXTURE_2D, historyTextures[i]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindFramebuffer(GL_FRAMEBUFFER, historyFBOs[i]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, historyTextures[i], 0);
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glBindTexture(GL_TEXTURE_2D, 0);

    blur_res_width = width;
    blur_res_height = height;
    pingPongIndex = 0;
    isFirstFrame = true;
}

void apply_motion_blur(int width, int height) {
    if (width != blur_res_width || height != blur_res_height || rawTexture == 0) {
        initializeMotionBlurResources(width, height);
    }

    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);

    glBindTexture(GL_TEXTURE_2D, rawTexture);
    glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 0, 0, width, height, 0);

    int curr = pingPongIndex;
    int prev = 1 - pingPongIndex;

    if (isFirstFrame) {
        glBindFramebuffer(GL_FRAMEBUFFER, historyFBOs[curr]);
        glViewport(0, 0, width, height);

        glUseProgram(drawShaderProgram);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, rawTexture);
        glUniform1i(drawTextureLoc, 0);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

        glEnableVertexAttribArray(drawPosLoc);
        glVertexAttribPointer(drawPosLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), nullptr);
        glEnableVertexAttribArray(drawTexCoordLoc);
        glVertexAttribPointer(drawTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, nullptr);

        isFirstFrame = false;
    } else {
        glBindFramebuffer(GL_FRAMEBUFFER, historyFBOs[curr]);
        glViewport(0, 0, width, height);

        glUseProgram(blendShaderProgram);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, rawTexture);
        glUniform1i(blendCurrentLoc, 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, historyTextures[prev]);
        glUniform1i(blendHistoryLoc, 1);

        // ImGui::SliderFloat("BLUR STRENGTH", &blur_strength, 0.0f, 1.0f, "%.2f");
        glUniform1f(blendFactorLoc, blur_strength);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

        glEnableVertexAttribArray(blendPosLoc);
        glVertexAttribPointer(blendPosLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), nullptr);
        glEnableVertexAttribArray(blendTexCoordLoc);
        glVertexAttribPointer(blendTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, nullptr);
    }

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, width, height);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glUseProgram(drawShaderProgram);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, historyTextures[curr]);
    glUniform1i(drawTextureLoc, 0);

    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

    glEnableVertexAttribArray(drawPosLoc);
    glVertexAttribPointer(drawPosLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), nullptr);
    glEnableVertexAttribArray(drawTexCoordLoc);
    glVertexAttribPointer(drawTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, nullptr);

    pingPongIndex = prev;
}

static int g_width = 0, g_height = 0;
static EGLContext g_targetcontext = EGL_NO_CONTEXT;
static EGLSurface g_targetsurface = EGL_NO_SURFACE;
static EGLBoolean (*orig_eglswapbuffers)(EGLDisplay, EGLSurface) = nullptr;

static void render() {
    GLint last_active_texture; glGetIntegerv(GL_ACTIVE_TEXTURE, &last_active_texture);
    glActiveTexture(GL_TEXTURE0);
    GLint last_tex0; glGetIntegerv(GL_TEXTURE_BINDING_2D, &last_tex0);
    glActiveTexture(GL_TEXTURE1);
    GLint last_tex1; glGetIntegerv(GL_TEXTURE_BINDING_2D, &last_tex1);
    glActiveTexture(last_active_texture);

    GLint last_prog; glGetIntegerv(GL_CURRENT_PROGRAM, &last_prog);
    GLint last_array_buffer; glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &last_array_buffer);
    GLint last_element_array_buffer; glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, &last_element_array_buffer);
    GLint last_fbo; glGetIntegerv(GL_FRAMEBUFFER_BINDING, &last_fbo);
    GLint last_viewport[4]; glGetIntegerv(GL_VIEWPORT, last_viewport);
    GLboolean last_scissor = glIsEnabled(GL_SCISSOR_TEST);
    GLboolean last_depth = glIsEnabled(GL_DEPTH_TEST);
    GLboolean last_blend = glIsEnabled(GL_BLEND);

    if (motion_blur_enabled) {
        apply_motion_blur(g_width, g_height);
    }

    glUseProgram(last_prog);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, last_tex0);
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, last_tex1);
    glActiveTexture(last_active_texture);

    glBindBuffer(GL_ARRAY_BUFFER, last_array_buffer);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, last_element_array_buffer);
    glBindFramebuffer(GL_FRAMEBUFFER, last_fbo);
    glViewport(last_viewport[0], last_viewport[1], last_viewport[2], last_viewport[3]);
    if (last_scissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
    if (last_depth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
    if (last_blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);

    if (blendPosLoc >= 0) glDisableVertexAttribArray(blendPosLoc);
    if (blendTexCoordLoc >= 0) glDisableVertexAttribArray(blendTexCoordLoc);
    if (drawPosLoc >= 0) glDisableVertexAttribArray(drawPosLoc);
    if (drawTexCoordLoc >= 0) glDisableVertexAttribArray(drawTexCoordLoc);
}

static EGLBoolean hook_eglswapbuffers(EGLDisplay dpy, EGLSurface surf) {
    if (!orig_eglswapbuffers) return EGL_FALSE;
    EGLContext ctx = eglGetCurrentContext();
    if (ctx == EGL_NO_CONTEXT || (g_targetcontext != EGL_NO_CONTEXT && (ctx != g_targetcontext || surf != g_targetsurface)))
        return orig_eglswapbuffers(dpy, surf);

    EGLint w, h;
    eglQuerySurface(dpy, surf, EGL_WIDTH, &w);
    eglQuerySurface(dpy, surf, EGL_HEIGHT, &h);
    if (w < 100 || h < 100) return orig_eglswapbuffers(dpy, surf);

    if (g_targetcontext == EGL_NO_CONTEXT) { g_targetcontext = ctx; g_targetsurface = surf; }
    g_width = w; g_height = h;

    render();

    return orig_eglswapbuffers(dpy, surf);
}

static void* mainthread(void*) {
    sleep(3);
    GlossInit(true);
    GHandle hegl = GlossOpen("libEGL.so");

    if (hegl) {
        void* swap = (void*)GlossSymbol(hegl, "eglSwapBuffers", nullptr);
        if (swap) GlossHook(swap, (void*)hook_eglswapbuffers, (void**)&orig_eglswapbuffers);
    }

    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_origin_launcher_Launcher_inbuilt_XeloOverlay_nativemod_MotionBlurMod_setEnabled(
    JNIEnv*, jobject, jboolean enabled) {
    motion_blur_enabled = (enabled == JNI_TRUE);
    if (!motion_blur_enabled) isFirstFrame = true;
}

__attribute__((constructor))
void display_init() {
    pthread_t t;
    pthread_create(&t, nullptr, mainthread, nullptr);
}