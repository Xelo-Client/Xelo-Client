#include <jni.h>
#include <android/log.h>
#include <android/input.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES3/gl3.h>
#include <pthread.h>
#include <unistd.h>
#include <dlfcn.h>
#include <string.h>
#include <vector>
#include <algorithm>

#include "pl/Hook.h"
#include "pl/Gloss.h"

#include "ImGui/imgui.h"
#include "ImGui/backends/imgui_impl_opengl3.h"
#include "ImGui/backends/imgui_impl_android.h"

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
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uCurrentFrame;
uniform sampler2D uHistoryFrame;
uniform float uBlendFactor;
void main() {
    vec4 current = texture2D(uCurrentFrame, vTexCoord);
    vec4 history = texture2D(uHistoryFrame, vTexCoord);
    gl_FragColor = mix(current, history, uBlendFactor);
}
)";

const char* drawFragmentShaderSource = R"(
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;
void main() {
    gl_FragColor = texture2D(uTexture, vTexCoord);
}
)";

static bool motion_blur_enabled = false;
static float blur_strength = 0.85f;

static GLuint rawTexture = 0;
static GLuint historyTextures[2] = {0, 0};
static GLuint historyFBOs[2] = {0, 0};
static int pingPongIndex = 0;

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

        GLfloat vertices[] = { -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f };
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

    glGenTextures(2, historyTextures);
    glGenFramebuffers(2, historyFBOs);

    for (int i = 0; i < 2; i++) {
        glBindTexture(GL_TEXTURE_2D, historyTextures[i]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        glBindFramebuffer(GL_FRAMEBUFFER, historyFBOs[i]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, historyTextures[i], 0);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glBindTexture(GL_TEXTURE_2D, 0);

    blur_res_width = width;
    blur_res_height = height;
    pingPongIndex = 0;
}

void apply_motion_blur(int width, int height) {
    if (width != blur_res_width || height != blur_res_height || rawTexture == 0) {
        initializeMotionBlurResources(width, height);
    }

    int curr = pingPongIndex;
    int prev = 1 - pingPongIndex;

    glBindTexture(GL_TEXTURE_2D, rawTexture);
    glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 0, 0, width, height, 0);

    glBindFramebuffer(GL_FRAMEBUFFER, historyFBOs[curr]);
    glViewport(0, 0, width, height);
    glClear(GL_COLOR_BUFFER_BIT);

    glUseProgram(blendShaderProgram);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, rawTexture);
    glUniform1i(blendCurrentLoc, 0);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, historyTextures[prev]);
    glUniform1i(blendHistoryLoc, 1);

    glUniform1f(blendFactorLoc, blur_strength);

    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

    glEnableVertexAttribArray(blendPosLoc);
    glVertexAttribPointer(blendPosLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), nullptr);
    glEnableVertexAttribArray(blendTexCoordLoc);
    glVertexAttribPointer(blendTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, nullptr);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, width, height);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glUseProgram(drawShaderProgram);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, historyTextures[curr]);
    glUniform1i(drawTextureLoc, 0);

    glEnableVertexAttribArray(drawPosLoc);
    glVertexAttribPointer(drawPosLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), nullptr);
    glEnableVertexAttribArray(drawTexCoordLoc);
    glVertexAttribPointer(drawTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, nullptr);

    pingPongIndex = prev;
}

static bool g_initialized = false;
static int g_width = 0, g_height = 0;
static EGLContext g_targetcontext = EGL_NO_CONTEXT;
static EGLSurface g_targetsurface = EGL_NO_SURFACE;
static EGLBoolean (*orig_eglswapbuffers)(EGLDisplay, EGLSurface) = nullptr;
static void (*orig_input1)(void*, void*, void*) = nullptr;
static int32_t (*orig_input2)(void*, void*, bool, long, uint32_t*, AInputEvent**) = nullptr;

static void hook_input1(void* thiz, void* a1, void* a2) {
    if (orig_input1) orig_input1(thiz, a1, a2);
    if (thiz && g_initialized) ImGui_ImplAndroid_HandleInputEvent((AInputEvent*)thiz);
}

static int32_t hook_input2(void* thiz, void* a1, bool a2, long a3, uint32_t* a4, AInputEvent** event) {
    int32_t result = orig_input2 ? orig_input2(thiz, a1, a2, a3, a4, event) : 0;
    if (result == 0 && event && *event && g_initialized) ImGui_ImplAndroid_HandleInputEvent(*event);
    return result;
}

static void drawmenu() {
    ImGui::SetNextWindowPos(ImVec2(10, 80), ImGuiCond_FirstUseEver);
    ImGui::SetNextWindowSize(ImVec2(350, 180), ImGuiCond_FirstUseEver);

    ImGui::Begin("Natural Motion Blur", nullptr);
    ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(16, 12));
    ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 10.0f);
    ImGui::SetWindowFontScale(1.2f);

    ImGui::Checkbox("Enable Motion Blur", &motion_blur_enabled);
    
    if (motion_blur_enabled) {
        ImGui::Spacing();
        ImGui::Text("Blur Strength (Trail Length)");
        ImGui::SliderFloat("##Strength", &blur_strength, 0.5f, 0.98f, "%.2f");
    }

    ImGui::PopStyleVar(2);
    ImGui::End();
}

static void setup() {
    if (g_initialized || g_width <= 0) return;
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.IniFilename = nullptr;
    io.FontGlobalScale = 1.4f;
    ImGui_ImplAndroid_Init();
    ImGui_ImplOpenGL3_Init("#version 300 es");
    g_initialized = true;
}

static void render() {
    if (!g_initialized) return;

    GLint last_prog; glGetIntegerv(GL_CURRENT_PROGRAM, &last_prog);
    GLint last_tex; glGetIntegerv(GL_TEXTURE_BINDING_2D, &last_tex);
    GLint last_array_buffer; glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &last_array_buffer);
    GLint last_element_array_buffer; glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, &last_element_array_buffer);
    GLint last_fbo; glGetIntegerv(GL_FRAMEBUFFER_BINDING, &last_fbo);
    GLint last_viewport[4]; glGetIntegerv(GL_VIEWPORT, last_viewport);

    if (motion_blur_enabled) {
        apply_motion_blur(g_width, g_height);
    }

    ImGuiIO& io = ImGui::GetIO();
    io.DisplaySize = ImVec2((float)g_width, (float)g_height);
    ImGui_ImplOpenGL3_NewFrame();
    ImGui_ImplAndroid_NewFrame(g_width, g_height);
    ImGui::NewFrame();
    
    drawmenu();
    
    ImGui::Render();
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

    glUseProgram(last_prog);
    glBindTexture(GL_TEXTURE_2D, last_tex);
    glBindBuffer(GL_ARRAY_BUFFER, last_array_buffer);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, last_element_array_buffer);
    glBindFramebuffer(GL_FRAMEBUFFER, last_fbo);
    glViewport(last_viewport[0], last_viewport[1], last_viewport[2], last_viewport[3]);
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
    
    setup();
    render();
    
    return orig_eglswapbuffers(dpy, surf);
}

static void hookinput() {
    void* sym = (void*)GlossSymbol(GlossOpen("libinput.so"), "_ZN7android13InputConsumer7consumeEPNS_26InputEventFactoryInterfaceEblPjPPNS_10InputEventE", nullptr);
    if (sym) GlossHook(sym, (void*)hook_input2, (void**)&orig_input2);
}

static void* mainthread(void*) {
    sleep(3);
    GlossInit(true);
    GHandle hegl = GlossOpen("libEGL.so");

    if (hegl) {
        void* swap = (void*)GlossSymbol(hegl, "eglSwapBuffers", nullptr);
        if (swap) GlossHook(swap, (void*)hook_eglswapbuffers, (void**)&orig_eglswapbuffers);
    }

    hookinput();
    return nullptr;
}

__attribute__((constructor))
void display_init() {
    pthread_t t;
    pthread_create(&t, nullptr, mainthread, nullptr);
}
