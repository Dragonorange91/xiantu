package com.longju.xiantu.combat;

import java.util.EnumSet;
import java.util.Set;

public final class ElementalRegression {
    private static int checks;
    private static void near(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 1e-5 || !Double.isFinite(actual))
            throw new AssertionError(label + ": " + actual + " != " + expected);
        checks++;
    }
    public static void main(String[] args) {
        Element[] cycle = {Element.METAL, Element.WOOD, Element.EARTH, Element.WATER, Element.FIRE};
        for (int i = 0; i < cycle.length; i++) for (int j = 0; j < cycle.length; j++) {
            double expected = j == (i + 1) % 5 ? 1.25 : i == (j + 1) % 5 ? .8 : 1;
            near(ElementalRules.matchup(cycle[i],cycle[j],1.25,.8),expected,"Five-element ordered pair " + i + "/" + j);
        }
        for (Element e : Element.values()) {
            near(ElementalRules.matchup(Element.THUNDER,e,1.25,.8),1,"Thunder attacks are neutral " + e);
            near(ElementalRules.matchup(e,Element.THUNDER,1.25,.8),1,"Thunder defense is neutral " + e);
            near(ElementalRules.matchup(Element.PHYSICAL,e,1.25,.8),1,"Physical is neutral " + e);
        }
        near(ElementalRules.matchup(Element.METAL,EnumSet.of(Element.WOOD,Element.FIRE),1.25,.8),1.025,"Mixed roots average, do not multiply");
        near(ElementalRules.matchup(Element.FIRE,Set.of(),1.25,.8),1,"No affinity is neutral");
        near(ElementalRules.resistance(.2,.1),.1,"Penetration subtracts resistance");
        near(ElementalRules.resistance(.8,1),-.2,"Penetration can expose weakness");
        near(ElementalRules.resistance(-.25,1),-.25,"Vulnerability floor");
        near(ElementalRules.resistance(3,-1),.8,"Independent bounds");
        near(ElementalRules.damage(100,Element.METAL,Set.of(Element.WOOD),.2,.1,1.25,.8),112.5,"Single hit combines matchup and effective resistance");
        near(ElementalRules.damage(100,Element.PHYSICAL,Set.of(Element.WOOD),.8,1,1.25,.8),100,"Physical ignores elemental attributes");
        near(ElementalRules.damage(100,Element.THUNDER,Set.of(Element.WATER),.2,0,1.25,.8),80,"Neutral thunder still respects thunder resistance");
        near(ElementalRules.damage(Float.NaN,Element.FIRE,Set.of(),0,0,1.25,.8),0,"NaN damage rejected");
        near(ElementalRules.damage(-1,Element.FIRE,Set.of(),0,0,1.25,.8),0,"Negative damage rejected");
        near(ElementalRules.damage(100,Element.FIRE,Set.of(),Double.NaN,Double.POSITIVE_INFINITY,1.25,.8),100,"Invalid attributes cannot create NaN");
        near(ElementalRules.matchup(Element.METAL,Element.WOOD,2,.6),2,"Configurable advantage");
        near(ElementalRules.matchup(Element.WOOD,Element.METAL,2,.6),.6,"Configurable reverse");
        near(ElementalRules.damage(100,Element.METAL,Set.of(Element.WOOD),.2,.1,1.25,.8,.25),103.125,"Only the elemental portion changes");
        near(ElementalRules.damage(100,Element.METAL,Set.of(Element.WOOD),.8,0,1.25,.8,0),100,"Zero elemental portion remains physical");
        near(ElementalRules.damage(100,Element.METAL,Set.of(Element.WOOD),.8,0,1.25,.8,Double.NaN),100,"Invalid fraction cannot create elemental damage");
        System.out.println("ElementalRegression PASS: " + checks + " checks");
    }
}
