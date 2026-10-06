/* Run inside the generated review page. Produces review documents, not runtime sprite edits. */
window.renderTemperatureReview=function(){
 const cv=document.createElement('canvas');cv.width=cv.height=1200;const c=cv.getContext('2d');c.imageSmoothingEnabled=false;
 const tx=(s,x,y,size=16,color='#e0e7d8')=>{c.font=`${size}px system-ui,"PingFang SC",sans-serif`;c.textBaseline='top';c.fillStyle=color;c.fillText(s,x,y);};
 const block=(x,y,w,h)=>{c.fillStyle='#223025';c.fillRect(x,y,w,h);};
 c.fillStyle='#121d17';c.fillRect(0,0,1200,1200);tx('WILDCRAFT · F02-01 · v01',30,25,13,'#d6b57b');tx('七档环境温度 HUD · 上暖下冷',30,57,32);tx('同一温度计 + 程序刻度与亮框 · 与采用料理HUD联合排布 · 布局模拟 / 字体近似',30,103,16,'#a9bba6');
 block(30,138,516,850);tx('七档 · 从上方极热到下方极冷',52,157,21);const stateImages={};
 for(let r=0;r<7;r++){
  const band=6-r,s={...M.presets.normal,band,edge:true},one=document.createElement('canvas');one.width=124;one.height=24;const oc=one.getContext('2d');oc.imageSmoothingEnabled=false;oc.fillStyle='#1a281f';oc.fillRect(0,0,124,24);drawTemperature(oc,s,{temperatureY:6,startY:20});
  const y=192+r*108;c.drawImage(one,8,1,108,20,58,y,432,80);tx(`档位 ${band} · ${T.states[band].zh} / ${T.states[band].en}`,60,y+84,12,'#a9bba6');stateImages[T.states[band].key]=one.toDataURL('image/png');
 }
 block(576,138,594,284);tx('温度计 v2 · 复核已有原生稿',597,156,21);tx('32 × 32 原生',619,203,15);tx('16 × 16 原生',824,203,15);c.drawImage(imgs.temperature32,618,230,128,128);c.drawImage(imgs.temperature,824,230,128,128);tx('顶部暖色 / 底部冷色；实时档位由旁边的亮框与文字表示',597,389,13,'#b1c0a9');
 block(576,448,594,540);tx('与已采用料理 HUD 联合排布',597,465,22);tx('左上局部 ×3 · 原生16px图标 · 当前环境示例：寒冷',597,502,13,'#b1c0a9');
 const joint=document.createElement('canvas');draw(joint,{...M.presets.normal,band:1,edge:true},1);c.drawImage(joint,0,0,174,146,597,534,522,438);
 tx('七档共享一个组件；刻度、标记、文字由程序绘制，无需七张运行贴图。',30,1021,18,'#d6b57b');
 tx('温度首先是环境信息：普通冷热不新增扣血、减速或精力惩罚。保暖辅助细雪；耐热不等于抗火。',30,1063,16,'#b9c9b3');
 tx('检查包括七档 × 多排心 / 精力隐藏 / 小视窗 / 中英文，以及专注时关闭低透明度边缘色调。',30,1100,16,'#b9c9b3');
 tx('温度图标与组件待采用，未接入游戏。红心与环境为示意；料理图标和布局使用已采用稿。',30,1161,14,'#9faf9a');
 const views={};for(const key of ['normal','hearts','hidden','english','short','focus']){const one=document.createElement('canvas');draw(one,{...M.presets[key],band:key==='focus'?6:1,edge:true},1);views[key]=one.toDataURL('image/png');}
 return {board:cv.toDataURL('image/png'),states:stateImages,views};
};
