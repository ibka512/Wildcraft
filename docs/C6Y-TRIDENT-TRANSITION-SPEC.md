# C6Y已采用三叉戟取收源准备：握点尚未合格

2026-10-13，正式48/25e4a04b与本机8a16e378保持，49/3dc6ef25完整31回归中。trident-transitions/v01已采用2026-10-10，32源/22骨，两身形默认back/waist .34 draw/.38 stow、empty/实际shield、左右主手；本阶段只source报告和锁定Minecraft本机模型核对，未Java renderer、未转换私有资源、未设50版本或正式标签。

源根/绑定/脚/头资格通过，实际main握点32源报告：default back16全部未过原.002源门槛，4段也超过既有native palm .015，最大standard stow_back_shield_R .026951309；v1 .002和v2 .015失败保留，v3只报告且明确NOT QUALIFIED，没有提高门槛称通过。源继承palm仍.125，设计explicit wrist .0625；须在已采样当前源wrist重算辅助掌心，再让实际trident_reference保持作者orientation/scale，只平移到当前作者wrist握点，以数值/GPU证明误差消除。不得改22皮肤关节、根、原物理或库存，也不能把仅提出的reanchor当通过。

当前source reader仅资格脚本扩展trident_reference palette，未改运行converter。原生MC26.3 TridentModel pole raw pixels y2..27，作者main anchor raw y17.6/16=1.10，secondary raw .70（相距.40），既有MeleeHands也使用1.10/.70；actual resolved native layer display/local必须核对。锁定TridentSpecialRenderer.submit本身直接submit原model、glint路径，DEFAULT_TRANSFORMATION=(1,-1,-1)，不能二次翻转或套剑sprite .23握点/轴。native special renderer/实际model和skin/pole握位/GPU尚待；源GLB观察网格/纹理不导入。

第一批只main TRIDENT、default flat stationary unarmored/capeless、原same-reference MELEE0 owner1/3 Settle .34/.38、现有instant新归属。source free support late draw/原生空副手二手握持需actual true pole与当前皮肤手距证明；携盾不借另一手。真实旧trident一次强击/native hit/wear/Fuse、原投掷/取消/新物品/GUI/hurt/use/盔甲/waist/原生特殊行为不改。未验证主握点/支撑/挂点/退出不能正式发布；握点源失败未解决可先做数值/当前native model菜单探针，保持所有runtime旧路径。

完整source资格不是玩法完成；复杂/waist/正式第一人称/C6整体仍待。所有源/派生资源/含数据JAR只本机，其他美术WD/历史保持，无新生产依赖/数据迁移/上传，待审13和另6不采用。

数值提案已实际跑back16×121=1936源采样：当前source wrist握点对true raw native pole1.10、secondary .70/.40轴，重设reference translation后native主握点与second轴误差<1e-12；22关节/源reference旋转与尺寸保持，原source误差不覆盖。只Raw geometry/数值提案，不是真实resolved item/GPU/副手皮肤握持。私有TridentTransitionRig草稿不在classpath；50 renderer/转换/JVM任务尚未启用。

空手另一臂源检查：standard R/L draw Back在进度 .65/.80/.88/.95 的当前palms到native secondary轴 .40点分别约.4021/.2548/.1882/.2481，只有作者结束端约4e-7接触。这是明确有绕行的late support路径，不是全过程双手实握；原source首末blend也不能据此宣称持续握持。若接入须逐阶段区分源辅助伸取、实际main pole握点和未完成的双手常驻握持/正式第一人称，不能把末帧资格当全流程contact通过。

原代码MeleeActions.release已核对：TRIDENT短按是普通light7 recovery/free，16刻才强击14精力；拟用真实短按一次native hit/wear1/Fuse31登记，不能沿AXE误断言wear2。主三叉戟原native右键使用優先于offShield，因此实际use退出必须used MAIN_HAND，而非复制剑/斧的offShield用法。测试私有草稿已按此准备，尚未执行/不在classpath。
