cmake_minimum_required(VERSION 3.4.1)

add_library(renderer SHARED Renderer.cpp)

find_library(log-lib log)
find_library(gles-lib glesv2)

target_link_libraries(renderer ${log-lib} ${gles-lib})
