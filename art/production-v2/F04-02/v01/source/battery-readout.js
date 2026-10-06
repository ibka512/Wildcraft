/* Review component: approved pixels + integer clipping, no runtime dependency. */
(function(root){
const CAPACITY=1000;
function energyValue(value){if(!Number.isFinite(value))throw new TypeError('Energy must be finite');return Math.max(0,Math.min(CAPACITY,Math.trunc(value)));}
function fractions(value){const e=energyValue(value);return [0,1,2].map(i=>Math.max(0,Math.min(1000,3*e-i*1000))/1000);}
function widths(value,regions){const e=energyValue(value);return regions.slice().reverse().map((r,i)=>Math.floor(r.w*Math.max(0,Math.min(1000,3*e-i*1000))/1000));}
function drawStatusIcon(ctx,empty,full,regions,value,x=0,y=0,scale=1){
 const e=energyValue(value),w=widths(e,regions);if(!Number.isInteger(scale)||scale<1)throw new RangeError('Use positive integer display scale');
 ctx.save();ctx.imageSmoothingEnabled=false;ctx.translate(x,y);ctx.scale(scale,scale);ctx.drawImage(empty,0,0);
 regions.slice().reverse().forEach((r,i)=>{if(w[i])ctx.drawImage(full,r.x,r.y,w[i],r.h,r.x,r.y,w[i],r.h);});ctx.restore();
 return {energy:e,widthsBottomUp:w,fractions:fractions(e)};
}
function reading(value,present=true,status=2){
 if(!present)return {present:false,energy:null,text:'— / 1000',tone:'neutral',semantic:'no_battery',showIcon:false,chargerStatus:0};
 const e=energyValue(value);return {present:true,energy:e,text:e+' / 1000',tone:e<100?'low':'normal',semantic:e===0?'empty':e===1000?'full':e<100?'low':'partial',showIcon:true,chargerStatus:status};
}
const GLYPHS={
 '0':['01110','10001','10011','10101','11001','10001','01110'],
 '1':['00100','01100','00100','00100','00100','00100','01110'],
 '2':['01110','10001','00001','00010','00100','01000','11111'],
 '3':['11110','00001','00001','01110','00001','00001','11110'],
 '4':['00010','00110','01010','10010','11111','00010','00010'],
 '5':['11111','10000','10000','11110','00001','00001','11110'],
 '6':['01110','10000','10000','11110','10001','10001','01110'],
 '7':['11111','00001','00010','00100','01000','01000','01000'],
 '8':['01110','10001','10001','01110','10001','10001','01110'],
 '9':['01110','10001','10001','01111','00001','00001','01110'],
 '/':['00001','00001','00010','00100','01000','10000','10000'],
 '—':['00000','00000','00000','11111','00000','00000','00000'],
 ' ':['00000','00000','00000','00000','00000','00000','00000']};
function drawNumber(ctx,text,x,y,color,scale=1){ctx.save();ctx.fillStyle=color;for(let i=0;i<text.length;i++){const g=GLYPHS[text[i]];if(!g)throw new Error('Unsupported numeric glyph');for(let row=0;row<7;row++)for(let col=0;col<5;col++)if(g[row][col]==='1')ctx.fillRect(x+(i*6+col)*scale,y+row*scale,scale,scale);}ctx.restore();return {width:Math.max(0,text.length*6-1)*scale,height:7*scale};}
function drawReadout(ctx,assets,regions,e,present=true,x=0,y=0,scale=1,theme='dark',status=2){
 const r=reading(e,present,status);let icon=null;if(r.showIcon)icon=drawStatusIcon(ctx,assets.empty,assets.full,regions,e,x,y,scale);
 const color=r.tone==='low'?(theme==='dark'?'#e5b96c':'#805014'):r.tone==='neutral'?(theme==='dark'?'#9da89f':'#637066'):(theme==='dark'?'#e3e9df':'#354438');
 const number=drawNumber(ctx,r.text,x+20*scale,y+3*scale,color,scale);return {...r,icon,number,color};
}
root.WildcraftBatteryReadout={CAPACITY,energyValue,fractions,widths,reading,drawStatusIcon,drawNumber,drawReadout};
if(typeof module!=='undefined')module.exports=root.WildcraftBatteryReadout;
})(typeof globalThis!=='undefined'?globalThis:this);
