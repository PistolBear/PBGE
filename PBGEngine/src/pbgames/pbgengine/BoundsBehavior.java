/** 
 * BoundsBehavior Class
 * 
 * A single class to handle Sprites nearing the edge of the screen for most games.
 * May not always apply, but often will be useful when user screens are fixed on
 * specific actors, sections, etc.
 */
package pbgames.pbgengine;

import android.graphics.Point;
import android.graphics.RectF;
import android.renderscript.Float2;
import android.util.Log;

public class BoundsBehavior extends Animation {
	private RectF p_bounds;
	private Float2 p_velocity;
	private Point p_size;
	enum p_boundBehaviorType {
		BOUNCE, DESTROY, WRAPAROUND, FENCE, CUSTOM
	};
	public p_boundBehaviorType behaviorType;

	public BoundsBehavior(RectF bounds) {
		p_bounds = bounds;
		animating = true;
		
		// If Sprite accidentally gets assigned behaviors that don't make sense,
		// don't crash, but print them in the log for debugging later.
		p_velocity = super.adjustVelocity(new Float2(0,0));
		p_size = new Point(0,0);
		
	}

	public BoundsBehavior(RectF bounds, Float2 v, Point p) {
		p_bounds = bounds;
		p_velocity = v;
		p_size = p;
		animating = true;
	}

	@Override
	public Float2 adjustPosition(Float2 original) {
		Float2 retval = new Float2(0,0);
		switch (behaviorType) {
			case BOUNCE:
				retval = bounceAction(original);
				break;
			case DESTROY:
				this.adjustActive(false);
				retval = new Float2(-100.0f,-100.0f);	// Put way off of the screen.  Flag to be destroyed should be handled.
				break;
			case WRAPAROUND:
				retval = wrapAroundAction(original);
				break;
			case FENCE:
			case CUSTOM:	// will utilize with scripting language
			default:
				retval =  fenceAction(original);
				break;
		}
		
		return retval;
	}
	
	private Float2 bounceAction(Float2 original) {
		Float2 modified = original;
        modified.x += p_velocity.x;
        modified.y += p_velocity.y;
        
        if (modified.x < p_bounds.left)
            p_velocity.x *= -1;
        else if (modified.x > p_bounds.right-p_size.x)
            p_velocity.x *= -1;
        
        if (modified.y < p_bounds.top)
            p_velocity.y *= -1;
        else if (modified.y > p_bounds.bottom-p_size.y)
            p_velocity.y *= -1;
        
		return modified;
	}

	/**
	 * fenceAction method
	 * fenceAction is used as the default for BoundsBehavior because it will VERY QUICKLY
	 * let debuggers know that they need to be doing something different with all of the
	 * sprites that will likely accumulate near the edge of the screen.
	 * 
	 * @author PistolBear
	 * @params Float2 original => Requested new position, pushes back to screen
	 * if invalid.
	 * 
	 */
	public Float2 fenceAction(Float2 original) {
		Float2 modified = original;

		if (modified.x < p_bounds.left)
			modified.x = p_bounds.left;
		else if (modified.x > p_bounds.right)
			modified.x = p_bounds.right;
		if (modified.y < p_bounds.top)
			modified.y = p_bounds.top;
		else if (modified.y > p_bounds.bottom)
			modified.y = p_bounds.bottom;

		return modified;
	}

	public Float2 wrapAroundAction(Float2 original) {
		if (p_velocity.equals(new Float2 (0,0)) || p_size.equals(new Point(0,0))) {
			Log.d("BoundsBehavior", "wrapAroundAction() CALLED WITHOUT VELOCITY/SIZE VALUES");	
			//Maybe should add in something above to track WHICH Sprite offended with this message
			return fenceAction(original);
			//Always default to fenceAction
		}
		
		else
		{
		Float2 modified = original;
		modified.x += p_velocity.x;
		modified.y += p_velocity.y;

		if (modified.x < p_bounds.left)
			modified.x = p_bounds.right - p_size.x;
		else if (modified.x > p_bounds.right - p_size.x)
			modified.x = p_bounds.left;

		if (modified.y < p_bounds.top)
			modified.y = p_bounds.bottom - p_size.y;
		else if (modified.y > p_bounds.bottom - p_size.y)
			modified.y = p_bounds.top;

		return modified;
		}
	}
}
