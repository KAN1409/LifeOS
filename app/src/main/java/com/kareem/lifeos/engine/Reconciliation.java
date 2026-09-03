package com.kareem.lifeos.engine;
public final class Reconciliation {
    public double score(String a,String b,long ta,long tb){
        if(a==null||b==null)return 0.0; String x=norm(a),y=norm(b); double text=x.equals(y)?1.0:(x.contains(y)||y.contains(x)?0.75:0.0); long dt=Math.abs(ta-tb); double time=dt<5000?1.0:(dt<30000?0.8:(dt<120000?0.4:0.0)); return text*0.75+time*0.25;
    }
    private String norm(String s){return s.trim().toLowerCase().replaceAll("\\s+"," ");}
}
