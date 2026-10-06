/* Small reference renderer; same materials and native silhouettes as the adopted review. */
(function(){
class Viewer{
 constructor(canvas){this.canvas=canvas;const g=this.gl=canvas.getContext('webgl',{alpha:false,antialias:true,preserveDrawingBuffer:true});if(!g)throw Error('WebGL unavailable');
  const shader=(type,code)=>{const s=g.createShader(type);g.shaderSource(s,code);g.compileShader(s);if(!g.getShaderParameter(s,g.COMPILE_STATUS))throw Error(g.getShaderInfoLog(s));return s;};
  const a=shader(g.VERTEX_SHADER,'attribute vec3 a;attribute vec3 c;varying vec3 color;void main(){gl_Position=vec4(a,1.0);color=c;}'),b=shader(g.FRAGMENT_SHADER,'precision mediump float;varying vec3 color;void main(){gl_FragColor=vec4(color,1.0);}');this.program=g.createProgram();g.attachShader(this.program,a);g.attachShader(this.program,b);g.linkProgram(this.program);g.deleteShader(a);g.deleteShader(b);if(!g.getProgramParameter(this.program,g.LINK_STATUS))throw Error('Shader link');this.buffer=g.createBuffer();
 }
 draw(state,view='third'){
  const scene=MotionReview.scene(state,view),g=this.gl,verts=[],aspect=this.canvas.width/this.canvas.height;
  const projected=v=>{
   if(view==='first'){const depth=-v[2];if(depth<=.5)return null;const f=Math.tan((state.fov??70)*Math.PI/360);return [v[0]/(depth*f*aspect),v[1]/(depth*f),Math.min(.99,depth/100)];}
   const p=WaistReview.pitch(WaistReview.yaw([v[0],v[1]-23,v[2]],state.yaw??-35),state.pitch??-8);return [p[0]/(29*aspect),p[1]/29,p[2]/100];
  };
  for(const mesh of scene.objects)for(const face of mesh.faces){const raw=face.indices.map(i=>mesh.vertices[i]),points=raw.map(projected);if(points.some(p=>p===null))continue;
   const a=raw[1].map((x,i)=>x-raw[0][i]),b=raw[2].map((x,i)=>x-raw[0][i]),n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],len=Math.hypot(...n)||1,shade=.72+.28*Math.abs((n[0]*-.35+n[1]*.7+n[2]*-.6)/len),color=face.color.map(c=>Math.min(1,c*shade));
   for(let i=1;i<points.length-1;i++)for(const p of [points[0],points[i],points[i+1]])verts.push(...p,...color);
  }
  g.viewport(0,0,this.canvas.width,this.canvas.height);g.clearColor(.067,.105,.092,1);g.clear(g.COLOR_BUFFER_BIT|g.DEPTH_BUFFER_BIT);g.enable(g.DEPTH_TEST);g.depthFunc(g.LEQUAL);g.useProgram(this.program);g.bindBuffer(g.ARRAY_BUFFER,this.buffer);g.bufferData(g.ARRAY_BUFFER,new Float32Array(verts),g.DYNAMIC_DRAW);
  for(const [key,offset]of [['a',0],['c',12]]){const loc=g.getAttribLocation(this.program,key);g.enableVertexAttribArray(loc);g.vertexAttribPointer(loc,3,g.FLOAT,false,24,offset);}g.drawArrays(g.TRIANGLES,0,verts.length/6);
  return {...scene,glError:g.getError(),triangles:verts.length/18};
 }
 dispose(){this.gl.deleteBuffer(this.buffer);this.gl.deleteProgram(this.program);this.gl.getExtension('WEBGL_lose_context')?.loseContext();}
}
window.MotionViewer=Viewer;
})();
