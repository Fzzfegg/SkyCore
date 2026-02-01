#version 120

uniform vec4 RColor;
uniform float angle;

varying vec4 texcoord;

float easeInOutCirc(in float x) {
  return x*x*x*x*x*x*x;
}

void main() {


    // 获取离中心点距离,大于的在圆外直接筛掉
    float dist = distance(vec2(0.5, 0.5), texcoord.st);
    if(dist >= 0.5){
      discard;
    }
    if(angle<=-1){
      dist = easeInOutCirc(dist*2);
      vec4 color = RColor;
      color.a = dist * color.a;
      if(color.a>0 && color.a<0.11){
        color.a=0.11;
      }
      gl_FragData[0] = color;
    }else{
      vec2 a = vec2(texcoord.s-0.5,texcoord.t-0.5);
      float dotValue = (a.y*a.y)/(a.x*a.x+a.y*a.y);
      if(a.y < 0){
        dotValue = 0 - dotValue;
      }
      if(dotValue < angle){
        discard;
      }

      float dist1 = easeInOutCirc(dist*2);

      float dist2 = 0;
      if(angle==0.0){
        dist2 = easeInOutCirc(1-distance(0.5, texcoord.t)*2);
      }else{
        dist2 = easeInOutCirc(1.0-(dotValue-angle));
      }



      vec4 color = RColor;
      color.a = max(color.a * max(dist1,dist2), 0.11);
      gl_FragData[0] = color;
    }
}
