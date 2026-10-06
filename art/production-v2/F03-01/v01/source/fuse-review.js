/* Review-only native geometry. This does not replace a Minecraft item model or renderer. */
(function(){
'use strict';
const bounds=m=>[0,1,2].map(i=>[Math.min(...m.vertices.map(v=>v[i])),Math.max(...m.vertices.map(v=>v[i]))]);
function placement(host,material,flight=false){
 const p=flight?SPEC.profiles.arrow.flight:SPEC.profiles[host],b=bounds(material),ext=b.map(v=>v[1]-v[0]),longest=Math.max(...ext);
 if(!Number.isFinite(longest)||longest<SPEC.guard.minimumLongestSide||longest>SPEC.guard.maximumLongestSide||material.vertices.some(v=>v.some(x=>!Number.isFinite(x))))return null;
 const scale=p.maxSpan/longest,center=b.map(v=>(v[0]+v[1])/2),axis=flight?0:1,offset=ext[axis]*scale/2-p.penetration,translate=center.map((v,i)=>p.contact[i]+p.normal[i]*offset-v*scale);
 return {scale,center,translate,maxSpan:p.maxSpan,contact:p.contact,penetration:p.penetration};
}
function box(a,b,c){const [x,y,z]=a,[X,Y,Z]=b;return {vertices:[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],faces:[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]].map(indices=>({indices,color:c}))};}
function mannequin(){const parts=[box([-.25,-.125,.75],[.25,.125,1.5],[.32,.4,.35]),box([-.25,-.25,1.5],[.25,.25,2],[.68,.63,.54])];for(const s of [-1,1]){parts.push(box([s*.375-.125,-.125,.75],[s*.375+.125,.125,1.5],[.32,.4,.35]));parts.push(box([s*.125-.125,-.125,0],[s*.125+.125,.125,.75],[.26,.30,.35]));}return parts;}
function rotateY(v,a){const c=Math.cos(a),s=Math.sin(a);return [v[0]*c+v[2]*s,v[1],-v[0]*s+v[2]*c];}
function geometry(host,mat,context,guide){const flight=context==='flight',material=MESH.materials[mat],p=placement(host,material,flight);if(!p)throw Error('Invalid reference material');
 const list=[{mesh:MESH.hosts[flight?'flight':host],transform:v=>v},{mesh:material,transform:v=>v.map((n,i)=>n*p.scale+p.translate[i])}];
 if(guide&&host==='shield'&&!flight){list.push({mesh:box([-.20,.1255,-.4],[.2,.1265,.4],[.24,.38,.48]),transform:v=>v});}
 let scale=1,angle=0,translation=[0,0,0],center=[0,0,0],radius=host==='shield'?1:.85;
 if(context==='ground'){scale=host==='shield'?.25:.5;radius=.65;}
 if(context==='held'||context==='back'){
  scale=SPEC.profiles[host].heldEffectiveScale;
  if(context==='back'){
   if(host==='arrow')throw Error('Arrows have no back record');const prof=BACK.profiles[host];translation=[prof.center[0]/16,prof.center[2]/16,prof.center[1]/16];angle=prof.canvasAngle*Math.PI/180;
  }else{translation=host==='shield'?[-.60,.37,1.08]:[-.6625,.28,1.0125];}
  center=[0,0,1];radius=1.4;
 }
 const objects=list.map(o=>({mesh:o.mesh,transform:v=>{let t=rotateY(o.transform(v).map(n=>n*scale),angle);return t.map((n,i)=>n+translation[i]-center[i]);}}));
 if(context==='held'||context==='back')objects.push(...mannequin().map(mesh=>({mesh,transform:v=>v.map((n,i)=>n-center[i])})));
 if(flight){center=[-.2,0,0];radius=.65;objects.forEach(o=>{const prev=o.transform;o.transform=v=>prev(v).map((n,i)=>n-center[i]);});}
 return {objects,p,radius,scale};
}
class Viewer{
 constructor(canvas){this.canvas=canvas;this.gl=canvas.getContext('webgl',{alpha:false,antialias:false,preserveDrawingBuffer:true});if(!this.gl)throw Error('WebGL unavailable');const g=this.gl;
 const shader=(type,src)=>{const s=g.createShader(type);g.shaderSource(s,src);g.compileShader(s);if(!g.getShaderParameter(s,g.COMPILE_STATUS))throw Error(g.getShaderInfoLog(s));return s;};
 const v=shader(g.VERTEX_SHADER,'attribute vec3 a;attribute vec3 c;varying vec3 color;void main(){gl_Position=vec4(a,1.0);color=c;}');const f=shader(g.FRAGMENT_SHADER,'precision mediump float;varying vec3 color;void main(){gl_FragColor=vec4(color,1.0);}');this.program=g.createProgram();g.attachShader(this.program,v);g.attachShader(this.program,f);g.linkProgram(this.program);g.deleteShader(v);g.deleteShader(f);if(!g.getProgramParameter(this.program,g.LINK_STATUS))throw Error('Link failed');this.buffer=g.createBuffer();this.last=null;
 }
 draw(host,mat,context,yaw=0,pitch=6,guide=false){const g=this.gl,scene=geometry(host,mat,context,guide),verts=[],ya=yaw*Math.PI/180,pa=pitch*Math.PI/180,cy=Math.cos(ya),sy=Math.sin(ya),cp=Math.cos(pa),sp=Math.sin(pa),aspect=this.canvas.width/this.canvas.height;
 const rot=v=>{let x=v[0]*cy-v[1]*sy,y=v[0]*sy+v[1]*cy,z=v[2];return [x,y*cp-z*sp,y*sp+z*cp];};
 for(const o of scene.objects){const points=o.mesh.vertices.map(v=>rot(o.transform(v)));
  for(const face of o.mesh.faces){const p=face.indices.map(i=>points[i]),u=p[1].map((n,i)=>n-p[0][i]),v=p[2].map((n,i)=>n-p[0][i]),n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]],len=Math.hypot(...n)||1,shade=.72+.28*Math.abs((n[0]*-.25+n[1]*.8+n[2]*.55)/len),col=face.color.map(c=>Math.min(1,c*shade));
   for(let i=1;i<p.length-1;i++)for(const point of [p[0],p[i],p[i+1]])verts.push(-point[0]/(scene.radius*aspect),point[2]/scene.radius,-point[1]/4,...col);
  }
 }
 g.viewport(0,0,this.canvas.width,this.canvas.height);g.clearColor(.075,.11,.10,1);g.clear(g.COLOR_BUFFER_BIT|g.DEPTH_BUFFER_BIT);g.enable(g.DEPTH_TEST);g.depthFunc(g.LEQUAL);g.useProgram(this.program);g.bindBuffer(g.ARRAY_BUFFER,this.buffer);g.bufferData(g.ARRAY_BUFFER,new Float32Array(verts),g.DYNAMIC_DRAW);
 for(const [key,offset] of [['a',0],['c',12]]){const a=g.getAttribLocation(this.program,key);g.enableVertexAttribArray(a);g.vertexAttribPointer(a,3,g.FLOAT,false,24,offset);}
 g.drawArrays(g.TRIANGLES,0,verts.length/6);this.last={host,material:mat,context,yaw,pitch,scale:scene.scale,materialScale:scene.p.scale,maxSpan:scene.p.maxSpan,triangles:verts.length/18,glError:g.getError()};return this.last;
 }
}
window.FuseReview={bounds,placement,geometry,Viewer};
})();
