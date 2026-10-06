/* Review-only menu presentation; no item movement or fabricated energy system. */
(function(root){
const slots=[{id:0,x:26,y:35,role:'battery'}];for(let row=0;row<3;row++)for(let col=0;col<9;col++)slots.push({id:1+row*9+col,x:8+18*col,y:84+18*row,role:'inventory'});for(let col=0;col<9;col++)slots.push({id:28+col,x:8+18*col,y:142,role:'hotbar'});
const states=[
 {id:0,key:'empty',zh:'放入有限电池',en:'Insert a battery',color:'#637066',helpZh:'放入有限电池；充电槽为空，不表示电池电量为零。',helpEn:'Insert a finite battery. An empty slot is different from a battery with no charge.'},
 {id:1,key:'missing_source',zh:'缺少固定红石源',en:'No redstone source',color:'#805014',helpZh:'充电器需要紧邻已放置的红石块，任一相邻方向均可。手持红石块和红石信号不能供电。',helpEn:'Place a fixed redstone block beside the charger, on any of its six adjacent sides. Held blocks and redstone signals do not supply energy.'},
 {id:2,key:'charging',zh:'充电中',en:'Charging',color:'#354438',helpZh:'正在从相邻红石块获得供电，数字显示电池当前余量。',helpEn:'Receiving supply from an adjacent redstone block. The number shows the battery’s current charge.'},
 {id:3,key:'full',zh:'电量已满',en:'Fully charged',color:'#354438',helpZh:'电池余量为1000，可以取走使用；无电源时仍保持满电状态。',helpEn:'The battery holds 1000 units and is ready to use. A full battery stays full without a source.'},
 {id:4,key:'waiting',zh:'等待共享供电',en:'Waiting for supply',color:'#805014',helpZh:'红石源存在，正在等候共享供电。电池已有余量保留。',helpEn:'A redstone source is present. Waiting for its shared supply; the battery keeps its existing charge.'}
];
const fixtures={empty:{present:false,energy:0,source:false,receiving:false},missing_source:{present:true,energy:500,source:false,receiving:false},charging:{present:true,energy:500,source:true,receiving:true},full:{present:true,energy:1000,source:false,receiving:false},waiting:{present:true,energy:500,source:true,receiving:false}};
function deriveStatus(s){return !s.present?0:s.energy>=1000?3:!s.source?1:s.receiving?2:4;}
function chargeWidth(e,present=true){return present?Math.floor(105*Math.max(0,Math.min(1000,e))/1000):0;}
const labels={zh:{title:'充电器',battery:'电池',charge:'电量',inventory:'物品栏'},en:{title:'Charger',battery:'Battery',charge:'Charge',inventory:'Inventory'}};
const layout={size:[176,166],title:{x:8,y:7},batteryLabel:{x:14,y:22},chargeLabel:{x:78,y:22},readout:{x:54,y:29,numberX:74,numberY:32,width:114},bar:{x:55,y:48,width:105,height:5},status:{glyphX:10,glyphY:60,textX:22,textY:60,width:146,hover:{x:8,y:57,w:160,h:14}},inventoryLabel:{x:8,y:72},batterySlot:{x:26,y:35,w:16,h:16},previousStatusAnchor:[8,64],statusMovedForInventoryClearance:true};
root.WildcraftChargerUi={slots,states,fixtures,deriveStatus,chargeWidth,labels,layout};if(typeof module!=='undefined')module.exports=root.WildcraftChargerUi;
})(typeof globalThis!=='undefined'?globalThis:this);
