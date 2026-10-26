package com.longju.xiantu.wood;

/** Material rank remains distinct from player realm and item quality. */
public enum SpiritWoodType {
    QING_WEN_ELM("qing_wen_elm","青纹榆","青纹木",0,20,2,.125),
    CHI_XIN_BIRCH("chi_xin_birch","赤心桦","赤心木",1,30,4,.12),
    ZI_WEN_MAPLE("zi_wen_maple","紫纹枫","紫纹木",2,45,6,.06),
    XUAN_TIE_PINE("xuan_tie_pine","玄铁松","玄铁木",3,60,8,.025),
    JIN_SI_NANMU("jin_si_nanmu","金丝楠","金丝木",4,90,12,.01),
    ZI_FU_CEDAR("zi_fu_cedar","紫府杉","紫府木",5,120,16,.004),
    XING_WEN_WUTONG("xing_wen_wutong","星纹梧桐","星纹木",7,180,24,.0015),
    TIAN_QIONG_SANDALWOOD("tian_qiong_sandalwood","天穹檀","天穹木",8,240,32,.0005);
    public final String id,treeName,woodName;
    public final int realm,minutes,stones;
    public final double chance;
    SpiritWoodType(String id,String tree,String wood,int realm,int minutes,int stones,double chance) {
        this.id=id;this.treeName=tree;this.woodName=wood;this.realm=realm;this.minutes=minutes;this.stones=stones;this.chance=chance;
    }
    public int rank(){return ordinal()+1;}
    public long growthTicks(){return minutes*1200L;}
    public long energyCost(){return stones*com.longju.xiantu.item.SpiritStoneTier.values()[ordinal()].spiritualPower();}
    public int axeLevel(){return rank()==1?1:rank()<=4?2:3;}
    public int minimumHeight(){return rank()==6?100:rank()==7?120:rank()==8?160:Integer.MIN_VALUE;}
}
