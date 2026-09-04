package com.kareem.lifeos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Small dependency-free vector icon renderer for the locked LifeOS reference UI. */
final class LifeOsIconView extends View {
    static final String LIFE="life",HOME="home",TIMELINE="timeline",SEARCH="search",ASK="ask",FILTER="filter",HISTORY="history",MORE="more",MIC="mic",SEND="send",CHEVRON="chevron",SCHOOL="school",CAR="car",COMMITMENT="commitment",DECISION="decision",PEOPLE="people",FILE="file",PLACE="place",EVENT="event",ALERT="alert",ACTION="action",UPCOMING="upcoming",CHECK="check",ACTIVITY="activity";
    private final String icon;private final int color;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
    LifeOsIconView(Context c,String icon,int color){super(c);this.icon=icon==null?ACTIVITY:icon;this.color=color;p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);setWillNotDraw(false);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(getWidth(),getHeight());if(size<=0)return;float ox=(getWidth()-size)/2f,oy=(getHeight()-size)/2f;c.save();c.translate(ox,oy);c.scale(size/24f,size/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.85f);path.reset();draw(c);c.restore();}
    private void draw(Canvas c){switch(icon){
        case LIFE:c.drawCircle(12,12,8.2f,p);break;
        case HOME:path.moveTo(3.5f,10.8f);path.lineTo(12,3.7f);path.lineTo(20.5f,10.8f);c.drawPath(path,p);c.drawRoundRect(new RectF(5.5f,9.6f,18.5f,20.3f),1.6f,1.6f,p);path.reset();path.moveTo(10,20.2f);path.lineTo(10,14.5f);path.lineTo(14,14.5f);path.lineTo(14,20.2f);c.drawPath(path,p);break;
        case TIMELINE:for(float y:new float[]{6,12,18}){p.setStyle(Paint.Style.FILL);c.drawCircle(5,y,1.3f,p);p.setStyle(Paint.Style.STROKE);c.drawLine(9,y,20,y,p);}break;
        case SEARCH:c.drawCircle(10.2f,10.2f,5.8f,p);c.drawLine(14.6f,14.6f,20.2f,20.2f,p);break;
        case ASK:c.drawRoundRect(new RectF(3.3f,4.2f,20.7f,17.8f),4.2f,4.2f,p);path.moveTo(8.2f,17.8f);path.lineTo(6.4f,21);path.lineTo(12.1f,17.8f);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);c.drawCircle(16.8f,8.1f,1.1f,p);c.drawCircle(18.9f,6.1f,.6f,p);break;
        case FILTER:c.drawLine(4,6,20,6,p);c.drawLine(6.5f,12,17.5f,12,p);c.drawLine(9,18,15,18,p);break;
        case HISTORY:path.addArc(new RectF(4,4,20,20),-55,285);c.drawPath(path,p);path.reset();path.moveTo(4.1f,5.2f);path.lineTo(4.3f,10);path.lineTo(8.5f,7.6f);c.drawPath(path,p);c.drawLine(12,8,12,12,p);c.drawLine(12,12,15.2f,14.1f,p);break;
        case MORE:p.setStyle(Paint.Style.FILL);c.drawCircle(12,5.5f,1.45f,p);c.drawCircle(12,12,1.45f,p);c.drawCircle(12,18.5f,1.45f,p);break;
        case MIC:c.drawRoundRect(new RectF(8.2f,3,15.8f,14),3.8f,3.8f,p);path.moveTo(5.8f,11.5f);path.cubicTo(5.8f,17.8f,18.2f,17.8f,18.2f,11.5f);c.drawPath(path,p);c.drawLine(12,17.2f,12,21,p);c.drawLine(8.5f,21,15.5f,21,p);break;
        case SEND:path.moveTo(3.5f,11.5f);path.lineTo(20.5f,4.2f);path.lineTo(15.6f,20);path.lineTo(11.7f,13.3f);path.close();c.drawPath(path,p);c.drawLine(11.7f,13.3f,20.2f,4.4f,p);break;
        case CHEVRON:path.moveTo(8.5f,5.5f);path.lineTo(15,12);path.lineTo(8.5f,18.5f);c.drawPath(path,p);break;
        case SCHOOL:path.moveTo(2.8f,9.1f);path.lineTo(12,4.2f);path.lineTo(21.2f,9.1f);path.lineTo(12,14);path.close();c.drawPath(path,p);path.reset();path.moveTo(6.2f,11.1f);path.lineTo(6.2f,16.1f);path.cubicTo(9.3f,18.4f,14.7f,18.4f,17.8f,16.1f);path.lineTo(17.8f,11.1f);c.drawPath(path,p);c.drawLine(21.1f,9.3f,21.1f,15.8f,p);break;
        case CAR:c.drawRoundRect(new RectF(3.2f,9.1f,20.8f,17.8f),2.3f,2.3f,p);path.moveTo(6.1f,9.2f);path.lineTo(8.2f,5.8f);path.lineTo(15.8f,5.8f);path.lineTo(17.9f,9.2f);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);c.drawCircle(7.2f,18.5f,1.7f,p);c.drawCircle(16.8f,18.5f,1.7f,p);break;
        case COMMITMENT:c.drawRoundRect(new RectF(5,3,18.5f,21),1.8f,1.8f,p);c.drawLine(8.5f,8,15.5f,8,p);c.drawLine(8.5f,12,15.5f,12,p);path.moveTo(8.2f,16.2f);path.lineTo(10.4f,18.2f);path.lineTo(15.9f,14.2f);c.drawPath(path,p);break;
        case DECISION:path.moveTo(12,3.5f);path.lineTo(20.5f,12);path.lineTo(12,20.5f);path.lineTo(3.5f,12);path.close();c.drawPath(path,p);break;
        case PEOPLE:c.drawCircle(9,8,3,p);c.drawCircle(16.5f,9.2f,2.4f,p);path.moveTo(3.7f,20);path.cubicTo(4.2f,14.8f,13.8f,14.8f,14.3f,20);c.drawPath(path,p);path.reset();path.moveTo(13.2f,19.7f);path.cubicTo(13.7f,16.2f,20.1f,16.1f,20.5f,19.7f);c.drawPath(path,p);break;
        case FILE:path.moveTo(6,3);path.lineTo(14.5f,3);path.lineTo(19,7.5f);path.lineTo(19,21);path.lineTo(6,21);path.close();c.drawPath(path,p);c.drawLine(14.5f,3.2f,14.5f,7.7f,p);c.drawLine(14.5f,7.7f,18.8f,7.7f,p);c.drawLine(9,12,16,12,p);c.drawLine(9,16,16,16,p);break;
        case PLACE:c.drawCircle(12,9.2f,2.6f,p);path.moveTo(12,21);path.cubicTo(9.2f,17.2f,5.8f,13.7f,5.8f,9.3f);path.cubicTo(5.8f,1.6f,18.2f,1.6f,18.2f,9.3f);path.cubicTo(18.2f,13.7f,14.8f,17.2f,12,21);c.drawPath(path,p);break;
        case EVENT:c.drawRoundRect(new RectF(4,5.5f,20,20.5f),2.1f,2.1f,p);c.drawLine(4,9.5f,20,9.5f,p);c.drawLine(8,3.5f,8,7.4f,p);c.drawLine(16,3.5f,16,7.4f,p);p.setStyle(Paint.Style.FILL);c.drawCircle(8,13.5f,1,p);c.drawCircle(12,13.5f,1,p);c.drawCircle(16,13.5f,1,p);break;
        case ALERT:c.drawCircle(12,12,8.3f,p);c.drawLine(12,7.2f,12,13.4f,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,16.8f,1.2f,p);break;
        case ACTION:c.drawCircle(12,12,8.3f,p);path.moveTo(7.8f,12.2f);path.lineTo(10.8f,15.1f);path.lineTo(16.7f,8.7f);c.drawPath(path,p);break;
        case UPCOMING:c.drawCircle(12,12,8.3f,p);c.drawLine(12,7.5f,12,12.2f,p);c.drawLine(12,12.2f,15.6f,14.4f,p);break;
        case CHECK:path.moveTo(5,12.5f);path.lineTo(9.4f,17);path.lineTo(19,7);c.drawPath(path,p);break;
        default:c.drawCircle(12,12,7.5f,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,12,1.4f,p);break;
    }}
}
