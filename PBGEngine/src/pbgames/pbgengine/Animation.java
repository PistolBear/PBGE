/** 
 * Animation Class 
 * Requires game.engine.Engine to build.
 */
package pbgames.pbgengine;

import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.renderscript.Float2;

public class Animation {
    public boolean animating;
    
    public Animation() {
        animating = false;
    }
    
    // The following methods do nothing but return the original unless overridden.

    public int adjustFrame(int original) {
        return original;
    }
    
    public int adjustTransparency(int original) {
        return original;
    }
    
    public Float2 adjustScale(Float2 original) {
        return original;
    }
    
    public float adjustRotation(float original) {
        return original;        
    }

    public Float2 adjustPosition(Float2 original) {
        return original;
    }
    
    public Float2 adjustVelocity(Float2 original) {
        return original;
    }
    
    public boolean adjustActive(boolean original) {
        return original;
    }
    
    public ColorFilter adjustColor(ColorFilter original) {
    	return original;
    }

	public Paint adjustColor(Paint original) {
		return original;
	}
}


