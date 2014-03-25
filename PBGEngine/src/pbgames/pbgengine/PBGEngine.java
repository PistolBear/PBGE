package pbgames.pbgengine;

/**
 * PBGEngine
 * @author PistolBear
 * @email pistolbear@gmail.com
 * 
 * Starting point for PBGames
 * Currently handles:
 * -loading assets
 * -checking android version and sensors (if applicable)
 * -managing activities and threads
 * -debug modes
 * -2D/3D modes
 */

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.ListIterator;
import android.app.Activity;
import android.os.Bundle;
import android.renderscript.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;

/**
 * PBGEngine class
 */
public abstract class PBGEngine extends Activity implements Runnable,
		OnTouchListener {
	private SurfaceView p_surfaceView;
	private Canvas p_canvas;
	private Thread p_thread;
	private boolean p_running, p_paused, p_debugMode, p_2dMode;
	private int p_pauseCount;
	private Paint p_paintDefaultDraw, p_paintDefaultFont;
	private Typeface p_typeface;
	private Point[] p_touchPoints;
	private int p_numPoints;
	private long p_preferredFrameRate, p_sleepTime;
	private Point p_screenSize;
	private LinkedList<Sprite> p_group;

	/**
	 * PBGEngine default constructor see full constructor description
	 */
	public PBGEngine() {
		this(false);
	}

	public PBGEngine(boolean debug) {
		this(debug, true);
	}

	/**
	 * PBGEngine full constructor
	 * 
	 * @param boolean debug : defaults false
	 * @param boolean is2dModeOn : defaults true
	 */
	public PBGEngine(boolean debug, boolean is2dModeOn) {
		Log.d("PBGEngine", "PBGEngine constructor");
		p_surfaceView = null;
		p_canvas = null;
		p_thread = null;
		p_running = false;
		p_paused = false;
		p_paintDefaultDraw = null;
		p_paintDefaultFont = null;
		p_numPoints = 0;
		p_typeface = null;
		p_preferredFrameRate = 40;
		p_sleepTime = 1000 / p_preferredFrameRate;
		p_pauseCount = 0;
		p_group = new LinkedList<Sprite>();
		p_debugMode = debug;
		p_2dMode = is2dModeOn;
	}

	/**
	 * Abstract methods that must be implemented in the sub-class!
	 */
	public abstract void init();

	public abstract void load();

	public abstract void draw();

	public abstract void update();

	public abstract void collision(Sprite sprite);

	/**
	 * Activity.onCreate()
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("PBGEngine", "PBGEngine.onCreate start");

		// disable the title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// set default screen orientation
		// setScreenOrientation(ScreenModes.LANDSCAPE);

		/**
		 * Remember to call init() in the subclass!
		 */
		init();

		// create the view object
		p_surfaceView = new SurfaceView(this);
		setContentView(p_surfaceView);

		// turn on touch listening
		p_surfaceView.setOnTouchListener(this);

		// create the points array
		p_touchPoints = new Point[5];
		for (int n = 0; n < 5; n++) {
			p_touchPoints[n] = new Point(0, 0);
		}

		// create default Paint object for drawing styles
		p_paintDefaultDraw = new Paint();
		p_paintDefaultDraw.setColor(Color.WHITE);

		// create default Paint object for font settings
		p_paintDefaultFont = new Paint();
		p_paintDefaultFont.setColor(Color.BLACK);
		p_paintDefaultFont.setTextSize(24);

		// get the screen dimensions
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		p_screenSize = new Point(dm.widthPixels, dm.heightPixels);

		// Call abstract load method in sub-class!
		load();

		// launch the thread
		p_running = true;
		p_thread = new Thread(this);
		p_thread.start();

		Log.d("PBGEngine", "PBGEngine.onCreate end");
	}

	/**
	 * Runnable.run() - main loop
	 */
	@Override
	public void run() {
		Log.d("PBGEngine", "PBGEngine.run start");

		ListIterator<Sprite> iter = null, iterA = null, iterB = null;

		Timer frameTimer = new Timer();
		int frameCount = 0;
		int frameRate = 0;
		long startTime = 0;
		long timeDiff = 0;

		while (p_running) {
			// Process frame only if not paused
			if (p_paused)
				continue;

			// Calculate frame rate
			frameCount++;
			startTime = frameTimer.getElapsed();
			if (frameTimer.stopwatch(1000)) {
				frameRate = frameCount;
				frameCount = 0;

				// reset touch input count
				p_numPoints = 0;
			}

			// Call abstract update method in sub-class!
			update();

			/**
			 * Test for collisions in the sprite group. Note that this takes
			 * place outside of rendering.
			 */
			iterA = p_group.listIterator();
			while (iterA.hasNext()) {
				Sprite sprA = (Sprite) iterA.next();
				if (!sprA.getActive())
					continue;
				if (!sprA.getCollidable())
					continue;

				/*
				 * Improvement to prevent double collision testing
				 */
				if (sprA.getCollided())
					continue; // skip to next iterator

				// iterate the list again
				iterB = p_group.listIterator();
				while (iterB.hasNext()) {
					Sprite sprB = (Sprite) iterB.next();
					if (!sprB.getActive())
						continue;
					if (!sprB.getCollidable())
						continue;

					/*
					 * Improvement to prevent double collision testing
					 */
					if (sprB.getCollided())
						continue; // skip to next iterator

					// do not collide with itself
					if (sprA == sprB)
						continue;

					// Ignore sprites with same ID
					if (sprA.getIdentifier() == sprB.getIdentifier())
						continue;

					if (collisionCheckRectF(sprA, sprB)) {
						sprA.setCollided(true);
						sprA.setColliderSprite(sprB);
						sprB.setCollided(true);
						sprB.setColliderSprite(sprA);
						break; // exit while
					}
				}
			}

			// begin drawing
			if (beginDrawing()) {
				if (p_debugMode)
					Log.d("beginDrawing", "beginDrawing() start");

				// Call abstract draw method in sub-class!
				draw();

				/**
				 * Draw the p_group entities with transforms
				 */
				iter = p_group.listIterator();
				while (iter.hasNext()) {
					Sprite spr = (Sprite) iter.next();
					if (spr.getActive()) {
						spr.animate();
						spr.draw();
						if (p_debugMode)
							Log.d("beginDrawing's iterator", "p_group item "
									+ spr.getName());
					}
				}
				if (p_debugMode)
					Log.d("still in beginDrawing",
							"finished iterating through p_group");

				if (p_debugMode) {
					/**
					 * Print some engine debug info to device screen.
					 */
					int x = p_canvas.getWidth() - 150;

					p_canvas.drawText("PBGENGINE", x, 20, p_paintDefaultFont);
					p_canvas.drawText(toString(frameRate) + " FPS", x, 40,
							p_paintDefaultFont);
					p_canvas.drawText("Pauses: " + toString(p_pauseCount), x,
							60, p_paintDefaultFont);
					p_canvas.drawText("Receiving " + toString(getTouchInputs())
							+ " inputs right now.", x, 80, p_paintDefaultFont);
				}

				// done drawing
				endDrawing();
			}

			/*
			 * Do some cleanup: collision notification, removing 'dead' sprites
			 * from the list.
			 */
			iter = p_group.listIterator();
			Sprite spr = null;
			while (iter.hasNext()) {
				spr = (Sprite) iter.next();

				// remove from list if flagged
				if (!spr.getActive()) {
					iter.remove();
					continue;
				}

				// is collision enabled for this sprite?
				if (spr.getCollidable()) {

					// has this sprite collided with anything?
					if (spr.getCollided()) {

						// is the target a valid object?
						if (spr.getColliderSprite() != null) {

							/*
							 * External func call: notify game of collision
							 * (with validated offender)
							 */
							collision(spr);

							// reset offender
							spr.setColliderSprite(null);
						}

						// reset collided state
						spr.setCollided(false);

					}
				}
			}

			// Calculate frame update time and sleep if necessary
			timeDiff = frameTimer.getElapsed() - startTime;
			long updatePeriod = p_sleepTime - timeDiff;
			if (updatePeriod > 0) {
				try {
					Thread.sleep(updatePeriod);
				} catch (InterruptedException e) {
					if (p_debugMode)
						Log.d("while_in_run()",
								"exception: could not sleep L300ish: " + e);
				}
			}

		}// while
		Log.d("PBGEngine", "PBGEngine.run end");
		System.exit(RESULT_OK);
	}

	/**
	 * BEGIN RENDERING Verify that the surface is valid and then lock the
	 * canvas.
	 */
	private boolean beginDrawing() {
		if (!p_surfaceView.getHolder().getSurface().isValid()) {
			return false;
		}
		p_canvas = p_surfaceView.getHolder().lockCanvas();
		return true;
	}

	/**
	 * END RENDERING Unlock the canvas to free it for future use.
	 */
	private void endDrawing() {
		p_surfaceView.getHolder().unlockCanvasAndPost(p_canvas);
	}

	/**
	 * Activity.onResume event method
	 */
	@Override
	public void onResume() {
		Log.d("PBGEngine", "PBGEngine.onResume");
		super.onResume();
		p_paused = false;
	}

	/**
	 * Activity.onPause event method
	 */
	@Override
	public void onPause() {
		Log.d("PBGEngine", "PBGEngine.onPause");
		super.onPause();
		p_paused = true;
		p_pauseCount++;
	}

	/**
	 * OnTouchListener.onTouch event method
	 * 
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// count the touch inputs
		p_numPoints = event.getPointerCount();
		if (p_numPoints > 5)
			p_numPoints = 5;

		// store the input values
		for (int n = 0; n < p_numPoints; n++) {
			p_touchPoints[n].x = (int) event.getX(n);
			p_touchPoints[n].y = (int) event.getY(n);
		}
		return true;
	}

	/**
	 * Shortcut methods to duplicate existing Android methods.
	 */
	public void fatalError(String msg) {
		Log.e("A MOST TERRIBLE ERROR HAPPENED", msg);
		System.exit(0);
	}

	/**
	 * Drawing helpers
	 */
	public void drawTextDefaultSize(String text, int x, int y) {
		p_canvas.drawText(text, x, y, p_paintDefaultFont);
	}

	/**
	 * PBGEngine helper get/set methods for private properties.
	 */
	public Point getSize() {
		return p_screenSize;
	}

	public int getScreenWidth() {
		return p_screenSize.x;
	}

	public int getScreenHeight() {
		return p_screenSize.y;
	}

	public SurfaceView getView() {
		return p_surfaceView;
	}

	public Canvas getCanvas() {
		return p_canvas;
	}

	public void setFrameRate(int rate) {
		p_preferredFrameRate = rate;
		p_sleepTime = 1000 / p_preferredFrameRate;
	}

	public int getTouchInputs() {
		return p_numPoints;
	}

	public Point getTouchPoint(int index) {
		if (index > p_numPoints)
			index = p_numPoints;
		return p_touchPoints[index];
	}

	public void setDefaultDrawColor(int color) {
		p_paintDefaultDraw.setColor(color);
	}

	public void setDefaultTextColor(int color) {
		p_paintDefaultFont.setColor(color);
	}
	
	public void setDefaultTextColor(Paint paint) {
		p_paintDefaultFont.setColor(paint.getColor());
	}

	public void setDefaultTextSize(int size) {
		p_paintDefaultFont.setTextSize((float) size);
	}

	public void setDefaultTextSize(float size) {
		p_paintDefaultFont.setTextSize(size);
	}

	/**
	 * Font style helper
	 */
	public enum DefaultFontStyles {
		NORMAL(Typeface.NORMAL), BOLD(Typeface.BOLD), ITALIC(Typeface.ITALIC), BOLD_ITALIC(
				Typeface.BOLD_ITALIC);
		int value;

		DefaultFontStyles(int type) {
			this.value = type;
		}
	}

	public void setDefaultTextStyle(DefaultFontStyles style) {

		p_typeface = Typeface.create(Typeface.DEFAULT, style.value);
		p_paintDefaultFont.setTypeface(p_typeface);
	}

	/**
	 * Screen mode helper
	 */
	public enum ScreenModes {
		LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE), PORTRAIT(
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		int value;

		ScreenModes(int mode) {
			this.value = mode;
		}
	}

	public void setScreenOrientation(ScreenModes mode) {
		setRequestedOrientation(mode.value);
	}

	/**
	 * Round to a default 2 decimal places
	 */
	public double round(double value) {
		return round(value, 2);
	}

	/**
	 * Round to any number of decimal places
	 */
	public double round(double value, int precision) {
		try {
			BigDecimal bd = new BigDecimal(value);
			BigDecimal rounded = bd.setScale(precision,
					BigDecimal.ROUND_HALF_UP);
			return rounded.doubleValue();
		} catch (Exception e) {
			Log.e("PBGEngine", "round: error rounding number");
		}
		return 0;
	}

	/**
	 * String conversion helpers
	 */
	public String toString(int value) {
		return Integer.toString(value);
	}

	public String toString(float value) {
		return Float.toString(value);
	}

	public String toString(double value) {
		return Double.toString(value);
	}

	public String toString(Float2 value) {
		String s = "X:" + round(value.x) + "," + "Y:" + round(value.y);
		return s;
	}

	public String toString(Float3 value) {
		String s = "X:" + round(value.x) + "," + "Y:" + round(value.y) + ","
				+ "Z:" + round(value.z);
		return s;
	}

	public String toString(Point value) {
		Float2 f = new Float2(value.x, value.y);
		return toString((Float2) f);
	}

	public String toString(Rect value) {
		RectF r = new RectF(value.left, value.top, value.right, value.bottom);
		return toString((RectF) r);
	}

	public String toString(RectF value) {
		String s = "{" + round(value.left) + "," + round(value.top) + ","
				+ round(value.right) + "," + round(value.bottom) + "}";
		return s;
	}

	/**
	 * Entity grouping methods
	 */

	public void addToGroup(Sprite sprite) {
		p_group.add(sprite);
	}

	public boolean removeFromGroup(Sprite sprite) {
		if (p_group.remove(sprite))
			return true;
		return false;
	}

	public boolean removeFromGroup(String s) {
		int size = p_group.size();
		for (int n = 0; n <= size; n++) {
			if (p_group.get(n).getName() == s)
				p_group.remove(n);
			return true;
		}
		return false;
	}

	public void removeFromGroup(int index) {
		p_group.remove(index);
	}

	public boolean removeAllFromGroup(Sprite sprite) {
		for (int n = 0; n < p_group.size(); n++) {
			if (p_group.get(n).equals(sprite))
				removeFromGroup(sprite);
		}

		return true;
	}

	public boolean removeAllFromGroup(String s) {
		for (int n = 0; n < p_group.size(); n++) {
			if (p_group.get(n).getName() == s)
				removeFromGroup(p_group.get(n));
		}

		return true;
	}

	/**
	 * getGroupSize() Useful for obtaining the LinkedList<Sprite> group size
	 * 
	 * @return int
	 */
	public int getGroupSize() {
		return p_group.size();
	}

	// This function is applied to classes as they have need of them; not
	// everything will report while in debug mode
	// unless it has been shown a problem in the past.
	public boolean getDebugMode() {
		return p_debugMode;
	}

	/**
	 * Collision detection
	 */

	public boolean collisionCheckRectF(Sprite A, Sprite B) {
		boolean test = RectF.intersects(A.getBoundsScaledF(),
				B.getBoundsScaledF());
		return test;
	}

}
