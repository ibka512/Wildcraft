/* Review-only component; state indices mirror TemperatureRules.STATES. */
(function(root){
const states=[
 {key:'extreme_cold',zh:'极冷',en:'Very cold',color:'#7faecd',value:-2.8},
 {key:'cold',zh:'寒冷',en:'Cold',color:'#a4c8dd',value:-1.8},
 {key:'cool',zh:'偏冷',en:'Cool',color:'#bfd6da',value:-.8},
 {key:'comfortable',zh:'舒适',en:'Comfortable',color:'#cbd8bd',value:0},
 {key:'warm',zh:'偏暖',en:'Warm',color:'#ddc39a',value:.8},
 {key:'hot',zh:'炎热',en:'Hot',color:'#e5b37b',value:1.8},
 {key:'extreme_hot',zh:'极热',en:'Very hot',color:'#ef9a66',value:2.8}
];
const rules={iconX:12,iconOffsetY:-3,iconSize:16,gaugeX:32,gaugeOffsetY:-2,gaugeWidth:5,gaugeStep:2,gaugeBarHeight:1,labelX:44,labelOffsetY:0,backgroundX:10,backgroundOffsetY:-3,backgroundWidth:104,backgroundHeight:16};
function layout(s,meal){const band=Math.max(0,Math.min(6,s.band??3)),y=meal.temperatureY,activeRow=6-band;return {band,state:states[band].key,label:states[band][s.language],color:states[band].color,iconX:12,iconY:y-3,labelX:44,labelY:y,activeRow,marker:{x:31,y:y-3+2*activeRow,width:7,height:3},gauge:states.map((v,i)=>({band:6-i,x:32,y:y-2+2*i,width:5,height:1,active:6-i===band,color:states[6-i].color})),box:{x:10,y:y-3,width:104,height:16},mealStartY:meal.startY};}
function edge(s){const band=s.band??3,value=states[band].value;if(s.focus||s.edge===false||Math.abs(value)<=.5)return {visible:false,alpha:0,width:4};const level=value<0?s.levels[0]:s.levels[1];return {visible:true,alpha:Math.round(Math.min(14,Math.abs(value)*5)/(level+1)),width:4,color:value<0?'#78acd0':'#d49a62'};}
root.WildcraftTemperatureHud={states,rules,layout,edge};if(typeof module!=='undefined')module.exports=root.WildcraftTemperatureHud;
})(typeof globalThis!=='undefined'?globalThis:this);
