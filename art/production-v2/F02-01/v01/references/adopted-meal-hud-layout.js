/* Review-only layout model, measured in Minecraft logical GUI pixels. */
(function(root){
const effectOrder=['warmth','cooling','recovery'];
const labels={zh:{warmth:'保暖',cooling:'耐热',recovery:'精力恢复'},en:{warmth:'Warmth',cooling:'Cooling',recovery:'Recovery'}};
const presets={
 normal:{title:'三效果并存',w:426,h:240,rows:1,rowHeight:9,stamina:true,levels:[2,1,2],seconds:[127,84,56],language:'zh',absorption:false,focus:false},
 single:{title:'单一保暖效果',w:426,h:240,rows:1,rowHeight:9,stamina:true,levels:[2,0,0],seconds:[127,0,0],language:'zh',absorption:false,focus:false},
 hearts:{title:'多排红心与吸收心',w:320,h:180,rows:4,rowHeight:8,stamina:true,levels:[2,1,2],seconds:[127,84,56],language:'zh',absorption:true,focus:false},
 hidden:{title:'精力自动隐藏',w:426,h:240,rows:1,rowHeight:9,stamina:false,levels:[2,1,2],seconds:[127,84,56],language:'zh',absorption:false,focus:false},
 english:{title:'英文排布',w:426,h:240,rows:1,rowHeight:9,stamina:true,levels:[2,1,2],seconds:[127,84,56],language:'en',absorption:false,focus:false},
 short:{title:'小视窗紧凑排列',w:320,h:164,rows:2,rowHeight:9,stamina:true,levels:[2,1,2],seconds:[127,84,56],language:'zh',absorption:false,focus:false},
 focus:{title:'专注与临近结束',w:426,h:240,rows:1,rowHeight:9,stamina:true,levels:[2,1,2],seconds:[9,84,56],language:'zh',absorption:false,focus:true},
 expired:{title:'保暖结束后重排',w:426,h:240,rows:1,rowHeight:9,stamina:true,levels:[0,1,2],seconds:[0,84,56],language:'zh',absorption:false,focus:false},
 none:{title:'无料理效果',w:426,h:240,rows:1,rowHeight:9,stamina:false,levels:[0,0,0],seconds:[0,0,0],language:'zh',absorption:false,focus:false}
};
const rules={x:12,iconSize:16,fullWidth:148,fullRowHeight:16,fullRowStride:20,compactWidth:53,compactHeight:20,compactGap:6,compactRowStride:24,bottomReserve:46,temperatureOffsetVisible:29,temperatureOffsetHidden:5,mealOffset:14};
function timeLabel(seconds){const s=Math.max(0,Math.floor(seconds));return `${Math.floor(s/60)}:${String(s%60).padStart(2,'0')}`;}
function compute(s){
 const heartBottom=12+(s.rows-1)*s.rowHeight+9;
 const temperatureY=heartBottom+(s.stamina?29:5),startY=temperatureY+14,bottom=s.h-rules.bottomReserve;
 const list=effectOrder.map((key,i)=>({key,level:s.levels[i],seconds:s.seconds[i]})).filter(v=>v.level>0&&v.seconds>0);
 const availableW=s.w-24,fullHeight=list.length?16+20*(list.length-1):0;
 const aim={left:Math.floor(s.w/2)-10,right:Math.floor(s.w/2)+10,top:Math.floor(s.h/2)-10,bottom:Math.floor(s.h/2)+10};
 const fullCrossesAim=list.length&&12+148>aim.left&&12<aim.right&&startY<aim.bottom&&startY+fullHeight>aim.top;
 let mode='full',cols=1;
 if(list.length && (availableW<rules.fullWidth||startY+fullHeight>bottom||fullCrossesAim)){
  mode='compact';
  const compactAvailableW=startY<aim.bottom&&startY+68>aim.top?Math.min(availableW,aim.left-12):availableW;
  cols=Math.max(1,Math.min(list.length,Math.floor((compactAvailableW+6)/(53+6))));
 }
 const rows=mode==='full'?list.length:Math.ceil(list.length/cols),height=list.length?(mode==='full'?fullHeight:20+24*(rows-1)):0;
 // No overlay can displace hearts or cover the reserved XP/hotbar area.
 const fits=availableW>=(mode==='full'?148:53)&&(!list.length||startY+height<=bottom);
 const boxes=list.map((v,i)=>{const x=12+(mode==='compact'?(i%cols)*59:0),y=startY+(mode==='compact'?Math.floor(i/cols)*24:i*20);return {...v,x,y,width:mode==='full'?148:53,height:mode==='full'?16:20,iconX:x,iconY:y+(mode==='compact'?2:0),name:labels[s.language][v.key],levelText:v.level===2?'II':'I',timeText:timeLabel(v.seconds),timerX:mode==='full'?x+114:x+19,timerY:mode==='full'?y+4:y+10,nameX:x+20,nameY:y+4,levelX:x+19,levelY:y,lowTime:v.seconds<=10};});
 return {aim,heartBottom,temperatureY,startY,bottom,mode,cols,rows,height,fits,boxes,staminaLabelY:heartBottom+6,staminaTrackY:heartBottom+17};
}
root.WildcraftMealHud={effectOrder,labels,presets,rules,timeLabel,compute};
if(typeof module!=='undefined')module.exports=root.WildcraftMealHud;
})(typeof globalThis!=='undefined'?globalThis:this);
