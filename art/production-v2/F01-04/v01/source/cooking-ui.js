/* Review-only model: exact menu slot positions, live status/progress contract. */
(function(root){
const slots=[{id:0,x:26,y:33,role:'ingredient'},{id:1,x:46,y:33,role:'ingredient'},{id:2,x:66,y:33,role:'ingredient'},{id:3,x:46,y:58,role:'bowl'},{id:4,x:134,y:45,role:'output'}];
for(let row=0;row<3;row++)for(let col=0;col<9;col++)slots.push({id:5+row*9+col,x:8+col*18,y:100+row*18,role:'inventory'});
for(let col=0;col<9;col++)slots.push({id:32+col,x:8+col*18,y:158,role:'hotbar'});
const states=[
 {id:0,key:'unmatched',zh:'放入匹配食材',en:'Add matching ingredients',color:'#716b51',glyph:'ellipsis'},
 {id:1,key:'missing_heat',zh:'锅下需要点燃的营火',en:'Light a campfire below',color:'#966c35',glyph:'heat'},
 {id:2,key:'cooking',zh:'烹饪中',en:'Cooking',color:'#9c662f',glyph:'steam'},
 {id:3,key:'missing_bowl',zh:'放入空碗',en:'Add an empty bowl',color:'#966c35',glyph:'bowl'},
 {id:4,key:'output_waiting',zh:'请先取走成品',en:'Take the finished meal',color:'#527046',glyph:'check'}
];
const presets={
 unmatched:{status:0,progress:0,slots:{0:'potato'},heat:true,recipe:false,description:'只有土豆，配方未匹配；输出保持为空'},
 missing_heat:{status:1,progress:0,slots:{0:'beetroot',1:'potato',2:'carrot',3:'bowl'},heat:false,recipe:true,description:'已匹配保暖II并有空碗；锅下缺有效热源'},
 cooking:{status:2,progress:56,slots:{0:'beetroot',1:'potato',2:'carrot',3:'bowl'},heat:true,recipe:true,description:'实际进度56/100；提交前输出槽为空'},
 missing_bowl:{status:3,progress:0,slots:{0:'beetroot',1:'potato',2:'carrot'},heat:true,recipe:true,description:'已匹配食材，但缺少真实空碗'},
 output_waiting:{status:4,progress:0,slots:{4:'warming'},heat:true,recipe:false,description:'料理已提交至成品槽；示例输入各1件已消耗'}
};
const labels={zh:{title:'料理锅',ingredients:'食材',bowl:'空碗',output:'成品',inventory:'物品栏'},en:{title:'Cooking Pot',ingredients:'Ingredients',bowl:'Bowl',output:'Meal',inventory:'Inventory'}};
const rules={width:176,height:182,slotSize:16,progress:{x:92,y:49,maxWidth:30,height:5,cookTicks:100},title:{x:8,y:7},ingredients:{x:16,y:21},bowl:{x:67,y:61},output:{x:127,y:30},status:{x:24,y:77,width:144,glyphX:11,glyphY:78},inventory:{x:8,y:89}};
function displayProgress(p){return Math.floor(30*Math.max(0,Math.min(100,p))/100);}
function deriveStatus(s){return s.slots[4]?4:!s.recipe?0:s.slots[3]!=='bowl'?3:!s.heat?1:2;}
function itemRegion(id){const q=slots.find(s=>s.id===id);return {x:q.x,y:q.y,width:16,height:16};}
root.WildcraftCookingUi={slots,states,presets,labels,rules,displayProgress,deriveStatus,itemRegion};if(typeof module!=='undefined')module.exports=root.WildcraftCookingUi;
})(typeof globalThis!=='undefined'?globalThis:this);
