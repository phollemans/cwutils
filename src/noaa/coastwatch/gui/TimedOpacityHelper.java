/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.function.Consumer;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.MouseInfo;
import java.awt.event.MouseEvent;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

import java.util.logging.Logger;

/**
 * 
 * 
 * 
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class TimedOpacityHelper implements OpacityHelper {

  private static final Logger LOGGER = Logger.getLogger (TimedOpacityHelper.class.getName());

  private float minimumAlpha;
  private int maximumInactivity;
  private Timer inactivityTimer;
  private Animation animation;
  private float alpha;
  private Component component;

  ////////////////////////////////////////////////////////////

  private class Animation {

  	private float start, current, target, increment;
  	private Timer timer;
  	private Consumer<Float> update;

  	public Animation (float start, float target, int duration, Consumer<Float> update) {
  		this.start = start;
  		this.current = start;
  		this.target = target;
  		this.update = update;
  		increment = (target - start)/(duration/33.0f);
   	  timer = new Timer (33, event -> timerEvent());
  	} // Animation 

  	private void timerEvent() {
  		current = current + increment;
  		if ((increment < 0 && current < target) || (increment > 0 && current > target)) {
  			current = target;
  		} // if
  		if (current == target) timer.stop();
  		update.accept (current);
  	} // timerEvent

  	public void start() { timer.start(); }
  	public void stop() { timer.stop(); }
  	public float getTarget() { return (target); }

  } // Animation class

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new helper that updates the opacity of the specified container 
   * after a timeout period with no mouse movement.
   * 
   * @param component the component to update.  The component and its children
   * must use the helper to paint using the new opacity value via 
   * {@link #setupGraphics}.
   * @param minimumAlpha the minimum alpha value to use.
   * @param maximumInactivity the maximum inactivity period in milliseconds.
   */
  public TimedOpacityHelper (
  	Component component,
  	float minimumAlpha,
  	int maximumInactivity
  ) {

  	this.component = component;
  	this.minimumAlpha = minimumAlpha;
  	this.maximumInactivity = maximumInactivity;
  	alpha = minimumAlpha;
	  inactivityTimer = new Timer (maximumInactivity, event -> inactivityTimerEvent());
	  inactivityTimer.setRepeats (false);

    registerForMouseMovement (component);

  } // TimedOpacityHelper

  ////////////////////////////////////////////////////////////

  private void startFadeAnimation (float targetAlpha) { 

  	if (animation != null) animation.stop();
  	int duration = (int) (300*Math.abs (alpha - targetAlpha)/(1-minimumAlpha));
  	animation = new Animation (alpha, targetAlpha, duration, value -> { 
  		alpha = value;
  		if (alpha == 0) component.setVisible (false);
  		else repaintComponent();
  		if (alpha == targetAlpha) animation = null;
  	});
  	animation.start();

  } // startFadeAnimation

  ////////////////////////////////////////////////////////////

  private void inactivityTimerEvent () { 

  	if (!isMouseInComponent()) startFadeAnimation (minimumAlpha); 

  } // inactivityTimerEvent

  ////////////////////////////////////////////////////////////

  private void repaintComponent () {

  	component.repaint();

  } // repaintComponent

  ////////////////////////////////////////////////////////////

  private void registerForMouseMovement (
  	Component componentToRegister
  ) {

  	// First register the component itself for mouse movement.
    componentToRegister.addMouseMotionListener (new MouseInputAdapter() {
      public void mouseMoved (MouseEvent event) { mouseActivityEvent (event); }
      public void mouseDragged (MouseEvent event) { mouseActivityEvent (event); }
    });

    // If the component is a container, recursively register all its children
    // for mouse movements as well.
    if (componentToRegister instanceof Container) {
    	var container = (Container) componentToRegister;
    	for (var child : container.getComponents())
    		registerForMouseMovement (child);
    } // if

  } // registerForMouseMovement

  ////////////////////////////////////////////////////////////

  private boolean isMouseInComponent () {

		var location = component.getLocationOnScreen();
		var bounds = new Rectangle (location, component.getSize());
		var mouseLocation = MouseInfo.getPointerInfo().getLocation();
		return (bounds.contains (mouseLocation));

  } // isMouseInComponent

  ////////////////////////////////////////////////////////////

  private void mouseActivityEvent (MouseEvent event) {

	  // If the component is visible but not at full opacity, check to see if
	  // an animation to full visibility is runnuing.
	  if (component.isVisible() && alpha != 1) { 
	  	if (animation == null || (animation != null && animation.getTarget() != 1)) 
	  		startFadeAnimation (1);
    } // if

	  // Otherwise if the component is not visible, make it visible and fully
	  // opaque.
	  else if (!component.isVisible()) {
	  	alpha = minimumAlpha;
	    component.setVisible (true);  	
	  	startFadeAnimation (1);
	  } // else

	  // Finally, restart the inactivity timer that goes off after some amount
	  // of inactivity (ie: this method has not been called in a while).
	  inactivityTimer.restart();

  } // mouseActivityEvent

  ////////////////////////////////////////////////////////////

  @Override
  public void setOpacity (float alpha) { this.alpha = alpha; }

  @Override
  public float getOpacity() { return (alpha); }

  @Override
  public void setupGraphics (Graphics g) {

   if (alpha != 1) {
     ((Graphics2D) g).setComposite (AlphaComposite.getInstance (AlphaComposite.SRC_OVER, alpha));
    } // if

  } // setupGraphics

  ////////////////////////////////////////////////////////////

} // ComponentTranslucencyHelper
