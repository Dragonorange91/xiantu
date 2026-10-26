#version 150
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec3 localPosition;
out vec3 viewPosition;
out vec3 viewNormal;
void main(){
    localPosition=Position;
    vec4 view=ModelViewMat*vec4(Position,1.0);
    viewPosition=view.xyz;
    viewNormal=normalize(mat3(ModelViewMat)*Position);
    gl_Position=ProjMat*view;
}
