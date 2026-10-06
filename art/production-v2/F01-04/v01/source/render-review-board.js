window.renderCookingReview=function(){
 const cv=document.createElement('canvas');cv.width=1200;cv.height=1270;const c=cv.getContext('2d');c.imageSmoothingEnabled=false;
 const label=(t,x,y,size=18,color='#dce5d5')=>{c.fillStyle=color;c.font=`${size}px system-ui,-apple-system,sans-serif`;c.textBaseline='top';c.fillText(t,x,y);};
 const full=(key,lang='zh')=>{const a=document.createElement('canvas');a.width=176;a.height=182;drawPanel(a.getContext('2d'),{...U.presets[key],language:lang});return a;};
 c.fillStyle='#111c16';c.fillRect(0,0,1200,1270);label('WILDCRAFT · F01-04 · v01',30,22,13,'#d9b778');label('料理锅界面 · 原生 176×182',30,52,29);label('已采用锅体材料与料理外观 · 五槽位置保留 · 本图为排布模拟，字体近似',30,96,17,'#b6c6ac');
 c.fillStyle='#223025';c.fillRect(30,137,557,600);label('烹饪中 · 进度 56/100',48,151,20);c.drawImage(full('cooking'),45,185,528,546);
 c.fillStyle='#223025';c.fillRect(609,137,561,600);label('共享底图 · 不烘焙文字／状态／物品',630,151,20);c.drawImage(imgs.panel,640,185,352,364);label('3 食材 + 1 空碗 + 1 成品',630,571,20,'#d9b778');label('27 格库存 + 9 格快捷栏',630,605,18);label('深色金属边框 · 暖纸底 · 少量木／铜色',630,639,16);label('原生源稿 4 图层；一张底图共享五状态',630,670,16);label('锅下热源沿用原版；界面不增加燃料槽',630,701,16);
 label('五状态对照 · 上半区 ×2，下方库存共用',30,757,21);
 const keys=['unmatched','missing_heat','cooking','missing_bowl','output_waiting'];keys.forEach((key,i)=>{const col=i%3,row=Math.floor(i/3),x=30+col*390,y=794+row*217;c.fillStyle='#223025';c.fillRect(x,y,371,207);label(U.states[U.presets[key].status].zh,x+10,y+8,18);c.drawImage(full(key),0,0,176,88,x+9,y+31,352,176);});
 const x=810,y=1021;label('动态内容由程序绘制',x,y,19,'#d9b778');label('进度、五状态符号、提示文字',x,y+37,15);label('物品／等级／时长读取真实数据',x,y+68,15);label('烹饪中不提前显示成品',x,y+99,15);label('本项待采用，未接入游戏',30,1240,13,'#9fb39b');
 const scenes={};for(const st of U.states){scenes[st.key]=full(st.key).toDataURL('image/png');scenes[st.key+'-en']=full(st.key,'en').toDataURL('image/png');}
 return {board:cv.toDataURL('image/png'),scenes};
};
