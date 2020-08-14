//
// Created by chush on 2020/8/9.
//

#include <jni.h>
#include <GLES2/gl2.h>
#include <cmath>
#include <algorithm>
#include <ctime>

jlong JNICALL createNativeObject(JNIEnv *env, jclass clazz);

void JNICALL deleteNativeObject(JNIEnv *env, jclass clazz, jlong native_pointer);

void JNICALL initializeGL(JNIEnv *env, jclass clazz, jlong native_pointer);

void JNICALL resizeGL(JNIEnv *env, jclass clazz, jlong native_pointer, jint width, jint height);

void JNICALL paintGL(JNIEnv *env, jclass clazz, jlong native_pointer);

void JNICALL addParticle(JNIEnv *env, jclass clazz, jlong native_pointer, jfloat x, jfloat y);

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) == JNI_OK) {
        jclass clazz = env->FindClass("org/cszt0/hamiltonian/NativeRenderer");
        JNINativeMethod nativeMethod[] = {
                {"nativeAlloc",          "()J",    (void *) createNativeObject},
                {"nativeFree",           "(J)V",   (void *) deleteNativeObject},
                {"nativeSurfaceCreated", "(J)V",   (void *) initializeGL},
                {"nativeSurfaceChange",  "(JII)V", (void *) resizeGL},
                {"nativeDrawFrame",      "(J)V",   (void *) paintGL},
                {"addParticle",          "(JFF)V", (void *) addParticle}
        };
        // 使用这个函数注册
        env->RegisterNatives(clazz, nativeMethod, 6);
    }
    srand(time(nullptr));
    return JNI_VERSION_1_4;
}

static constexpr int particle_max_count = 8192;
static constexpr float speed = 0.01;

struct Particle {
    GLfloat position[2];
    GLfloat speed[2];
    GLfloat color[3];
    GLfloat start_time;
};

struct CppObject {
    Particle particles[particle_max_count];
    int particle_index;
    int particle_length;

    GLuint program;
    GLuint uTime;
    GLuint aPosition;
    GLuint aSpeed;
    GLuint aColor;
    GLuint aStartTime;

    float time;
};

inline void createProgram(CppObject *object) {
    static constexpr const char *vertex_shader_code =
            "uniform float u_Time;\n"
            "attribute vec2 a_Position;\n"
            "attribute vec2 a_Speed;\n"
            "attribute vec3 a_Color;\n"
            "attribute float a_StartTime;\n"
            "\n"
            "varying vec3 v_Color;\n"
            "varying float v_ElapsedTime;\n"
            "\n"
            "void main() {\n"
            "   v_Color = a_Color;\n"
            "   v_ElapsedTime = (u_Time - a_StartTime);\n"
            "   vec2 currentPosition = a_Position + a_Speed * v_ElapsedTime;\n"
            "   gl_Position = vec4(currentPosition, 0, 1);\n"
            "   gl_PointSize = 100.0;\n"
            "}\n";

    static constexpr const char *fragment_shader_code =
            "precision mediump float;\n"
            "varying vec3 v_Color;\n"
            "varying float v_ElapsedTime;\n"
            "\n"
            "void main() {\n"
            "   float lt = length(gl_PointCoord.xy - vec2(0, 0));\n"
            "   float rt = length(gl_PointCoord.xy - vec2(1, 0));\n"
            "   float lb = length(gl_PointCoord.xy - vec2(0, 1));\n"
            "   float rb = length(gl_PointCoord.xy - vec2(1, 1));\n"
            "   if (lt < 0.5 || rt < 0.5 || lb < 0.5 || rb < 0.5) {\n"
            "       discard;\n"
            "   } else {\n"
            "      gl_FragColor = vec4(v_Color, 1.0 / v_ElapsedTime);\n"
            "   }\n"
            "}\n";

    GLuint vertex_shader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertex_shader, 1, &vertex_shader_code, nullptr);
    glCompileShader(vertex_shader);

    GLuint fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragment_shader, 1, &fragment_shader_code, nullptr);
    glCompileShader(fragment_shader);

    GLuint program = glCreateProgram();
    glAttachShader(program, vertex_shader);
    glAttachShader(program, fragment_shader);
    glLinkProgram(program);

    glDeleteShader(vertex_shader);
    glDeleteShader(fragment_shader);

    object->program = program;
    object->uTime = glGetUniformLocation(program, "u_Time");
    object->aPosition = glGetAttribLocation(program, "a_Position");
    object->aSpeed = glGetAttribLocation(program, "a_Speed");
    object->aColor = glGetAttribLocation(program, "a_Color");
    object->aStartTime = glGetAttribLocation(program, "a_StartTime");
}

inline void
newParticle(CppObject *object, float x, float y, float angle, float red, float green, float blue) {

    auto &particle = object->particles[object->particle_index];
    particle.position[0] = x;
    particle.position[1] = y;
    particle.color[0] = red;
    particle.color[1] = green;
    particle.color[2] = blue;

    particle.speed[0] = speed * std::cos(angle);
    particle.speed[1] = speed * std::sin(angle);

    particle.start_time = object->time;

    object->particle_index = (object->particle_index + 1) % particle_max_count;
    object->particle_length = std::min(object->particle_length + 1, particle_max_count);
}

#define CPP_OBJECT auto object = reinterpret_cast<CppObject*>(native_pointer)

jlong createNativeObject(JNIEnv *env, jclass clazz) {
    auto object = new CppObject;
    object->particle_index = 0;
    object->particle_length = 0;
    object->time = 0;
    return reinterpret_cast<jlong>(object);
}

void deleteNativeObject(JNIEnv *env, jclass clazz, jlong native_pointer) {
    CPP_OBJECT;
    delete object;
}

void initializeGL(JNIEnv *env, jclass clazz, jlong native_pointer) {
    CPP_OBJECT;
    glClearColor(0, 0, 0.3, 0);
    createProgram(object);
}

void resizeGL(JNIEnv *env, jclass clazz, jlong native_pointer, jint width, jint height) {
    CPP_OBJECT;
    glViewport(0, 0, width, height);
}

void paintGL(JNIEnv *env, jclass clazz, jlong native_pointer) {
    CPP_OBJECT;
    glClear(GL_COLOR_BUFFER_BIT);

    object->time++;
    glUseProgram(object->program);
    glUniform1f(object->uTime, object->time);
    glVertexAttribPointer(object->aPosition, 2, GL_FLOAT, GL_FALSE, sizeof(Particle),
                          ((uint8_t *) object->particles) + offsetof(Particle, position));
    glEnableVertexAttribArray(object->aPosition);
    glVertexAttribPointer(object->aSpeed, 2, GL_FLOAT, GL_FALSE, sizeof(Particle),
                          ((uint8_t *) object->particles) + offsetof(Particle, speed));
    glEnableVertexAttribArray(object->aSpeed);
    glVertexAttribPointer(object->aColor, 3, GL_FLOAT, GL_FALSE, sizeof(Particle),
                          ((uint8_t *) object->particles) + offsetof(Particle, color));
    glEnableVertexAttribArray(object->aColor);
    glVertexAttribPointer(object->aStartTime, 1, GL_FLOAT, GL_FALSE, sizeof(Particle),
                          ((uint8_t *) object->particles) + offsetof(Particle, start_time));
    glEnableVertexAttribArray(object->aStartTime);

    glDrawArrays(GL_POINTS, 0, object->particle_length);
}

void JNICALL addParticle(JNIEnv *env, jclass clazz, jlong native_pointer, jfloat x, jfloat y) {
    CPP_OBJECT;

    switch (rand() % 3) {
        case 0:
            newParticle(object, x, y, rand(), 1, 0, 0);
            break;
        case 1:
            newParticle(object, x, y, rand(), 0, 1, 0);
            break;
        case 2:
            newParticle(object, x, y, rand(), 0, 0, 1);
            break;
    }
}