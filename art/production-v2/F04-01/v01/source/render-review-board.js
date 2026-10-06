window.renderBatteryReview=function(){
const cv=document.createElement('canvas');cv.width=1200;cv.height=1320;const c=cv.getContext('2d');c.imageSmoothingEnabled=false;
const label=(t,x,y,size=18,color='#dce5d5')=>{c.fillStyle=color;c.font=`${size}px system-ui,-apple-system,sans-serif`;c.textBaseline='top';c.fillText(t,x,y);};
c.fillStyle='#111c16';c.fillRect(0,0,1200,1320);label('WILDCRAFT · F04-01 · v01',30,23,13,'#d9b778');label('有限电池 · 金属外壳与三格红石能量窗',30,52,29);label('同一实际模型与尺寸 · 原生材质 / 独立16与32物品图 · 灯光为展示照明',30,99,17,'#b6c6ac');
for(const [i,key,txt] of [[0,'empty','空电 · 0 / 1000'],[1,'half','部分 · 500 / 1000'],[2,'full','满电 · 1000 / 1000']]){const x=30+i*390;c.fillStyle='#223025';c.fillRect(x,142,371,400);label(txt,x+12,153,20);c.drawImage(imgs[key],x+9,187,352,352);}
c.fillStyle='#223025';c.fillRect(30,563,753,258);label('原生物品图 · 32×32 与 16×16 独立排布',48,578,20);
for(const [i,e] of [0,500,1000].entries()){const x=56+i*235;B.drawIcon(c,imgs.icon32,REG.regions['32'],32,e,x,613,4);B.drawIcon(c,imgs.icon16,REG.regions['16'],16,e,x+135,645,4);label(`${e} / 1000`,x+25,753,16);}
label('同一壳体＋窗口遮罩，连续电量由程序表示；精确值保留',48,791,16,'#b6c6ac');
c.fillStyle='#223025';c.fillRect(803,563,367,258);label('沿用已采用电量HUD身份',821,578,19);c.drawImage(imgs.status32,826,626,128,128);c.drawImage(imgs.status16,978,642,96,96);label('铜接点 · 金属框 · 三格红石',821,769,16);label('旧HUD图标原件保留',821,796,14,'#b6c6ac');
for(const [x,w,key,txt] of [[30,360,'back','背面 · 平背与铜接点'],[410,400,'node','安装尺度 · 现主体灰盒'],[830,340,'socket','插座尺度 · 尚非充电器设计']]){c.fillStyle='#223025';c.fillRect(x,843,w,384);label(txt,x+10,854,18);const width=w-18,height=key==='node'?width*900/1100:width;c.drawImage(imgs[key],x+9,895,width,height);}
label('窗口随真实电量变化；外壳 / 接点 / 尺寸保持同一设计，无需逐百分比制作模型。',30,1248,17,'#d9b778');label('当前待采用，未接入游戏。机械与插座是尺寸示意；后续分别完成正式适配与充电器美术。',30,1287,13,'#9fb39b');
return cv.toDataURL('image/png');
};
