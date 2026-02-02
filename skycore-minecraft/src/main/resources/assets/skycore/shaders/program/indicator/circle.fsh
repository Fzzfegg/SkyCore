#version 120

uniform vec4 RColor;
uniform float angle;
uniform float useTextureMask;
uniform sampler2D DiffuseSampler;

varying vec4 texcoord;

float easeInOutCirc(in float x) {
  return x*x*x*x*x*x*x;
}

void main() {


    float customMask = 0.0;
    if(useTextureMask > 0.0){
      vec4 textureSample = texture2D(DiffuseSampler, texcoord.st);
      customMask = textureSample.a;
    }

    // 获取离中心点距离,大于的在圆外直接筛掉
    float dist = distance(vec2(0.5, 0.5), texcoord.st);
    float radial = dist;
    if(dist >= 0.5){
      discard;
    }
    float blend = clamp(useTextureMask, 0.0, 1.0);
    if(angle<=-1){
      float proceduralMask = easeInOutCirc(dist*2);
      vec4 color = RColor;
      float mask = mix(proceduralMask, customMask, blend);
      color.a = max(mask * color.a, 0.11);
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
      float proceduralMask = max(dist1, dist2);
      float finalMask = mix(proceduralMask, customMask, blend);
      color.a = max(color.a * finalMask, 0.11);
      gl_FragData[0] = color;
    }
}
