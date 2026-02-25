#version 120

uniform vec4 RColor;
uniform float width;
uniform float height;

varying vec4 texcoord;

float easeInOutCirc(in float x) {
  return x*x*x*x*x*x*x;
}
void main() {
    float w=width/2.0;
    float h=height/2.0;

    float distW = distance(0.5, texcoord.s);
    float distH = distance(0.5, texcoord.t);
    if(distW>w || distH>h){
      discard;
    }
    vec4 color = RColor;
    distW = easeInOutCirc(distW/w);
    distH = easeInOutCirc(distH/h);
    color.a = max(color.a * max(distW,distH), 0.11);

    gl_FragData[0] = color;
}
