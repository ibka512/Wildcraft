/* Review-only layout model. Existing runtime input rules; candidate 20px art row. */
(function(root){
const directions=['forward','front_right','right','back_right','backward','back_left','left','front_left'];
function direction(yaw,x,z){let a=yaw*Math.PI/180,f=-Math.sin(a)*x+Math.cos(a)*z,r=-Math.cos(a)*x-Math.sin(a)*z;return ((Math.round(Math.atan2(r,f)/(Math.PI/4))%8)+8)%8;}
function strength(speed){return speed<.012?'light':speed<.024?'moderate':'strong';}
function overlaps(a,b){return a.x<b.x+b.width&&a.x+a.width>b.x&&a.y<b.y+b.height&&a.y+a.height>b.y;}
function layout(s,textWidth){
 const w=s.w,h=s.h,rows=Math.ceil((s.health+s.absorption)/20),rh=rows>=4?8:9,heartBottom=12+(rows-1)*rh+9,temperatureY=heartBottom+(s.stamina?29:5),startY=temperatureY+14,bottom=h-46,aim={x:w/2-10,y:h/2-10,width:20,height:20};
 const n=s.meals?3:0,fullBottom=startY+(n-1)*20+16;
 const full=w>=172&&fullBottom<=bottom&&(160<=aim.x||fullBottom<=aim.y||startY>=aim.y+20);const cols=Math.max(1,Math.min(n||1,Math.floor((Math.min(w-12,aim.x)-12+6)/59)));
 let meals=Array.from({length:n},(_,i)=>({x:12+(full?0:i%cols*59),y:startY+(full?i*20:Math.floor(i/cols)*24),width:full?148:53,height:full?16:20,kind:i,compact:!full}));
 if(meals.some(b=>b.x+b.width>w-12||b.y+b.height>bottom||overlaps(b,aim)))meals=[];
 const y=Math.max(temperatureY+16,...meals.map(b=>b.y+b.height+4));
 const key=s.outdoors?strength(s.speed):'sheltered',wet=s.precipitation>.2;const fullWidth=(key==='sheltered'?20:40)+textWidth(key)+(wet?24+textWidth('wet'):0)+4,compactWidth=(key==='sheltered'?20:40)+(wet?20:0);
 const fits=width=>width<=w-24&&y+20<=bottom&&!overlaps({x:12,y,width,height:20},aim);
 let mode=fits(fullWidth)?'full':fits(compactWidth)?'compact':'hidden',reason=mode==='hidden'?'space':null;
 if(s.hidden){mode='hidden';reason='hud-hidden';}else if(!['glide','wing'].includes(s.activity)){mode='hidden';reason='inactive';}
 return{w,h,rows,rowHeight:rh,heartBottom,temperatureY,meals,aim,bottom,wind:{x:12,y,width:mode==='full'?fullWidth:compactWidth,height:20,mode,reason,key,wet,direction:direction(s.yaw,s.windX,s.windZ)},candidatePlacement:true};
}
root.WildcraftWindArt={directions,direction,strength,layout,overlaps};if(typeof module!=='undefined')module.exports=root.WildcraftWindArt;
})(typeof globalThis!=='undefined'?globalThis:this);
