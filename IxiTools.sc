
IxiWin {
	var win, canvas, run, frame, gap, ctrlw, down, keypressed;

	*new { |name="ixiapp", rect|
		^super.new;//.init(name, rect, path);
	}

	init {|name, rect|
		rect ?? {rect = Rect(0, 0, 1012, 740)};

		run = true;
		gap = 1/12.0;
		frame = 1;

		win = Window(name, rect, resizable: false);
		win.onClose = {
			this.close
		};
		win.view.keyDownAction_({ |char, modifiers, unicode, keycode, key|
			this.keyDown(char, modifiers, unicode, keycode, key)
		});
		win.view.keyUpAction_({ |char, modifiers, unicode, keycode, key|
			this.keyUp(char, modifiers, unicode, keycode, key)
		});

		~stagewidth = win.bounds.width.asFloat;
		~stageheight = win.bounds.height.asFloat;

		canvas = UserView(win, win.view.bounds.width@win.view.bounds.height)
		.canFocus_(false)
		.clearOnRefresh_(true) // no refresh when window is refreshed
		//.relativeOrigin_(true) // use this for the refresh
		.drawFunc_({ |view|
			this.draw;
		})
		.mouseMoveAction_({ |view, x, y, mod| // moved while down
			~mouseloc = Point(x,y);
			this.mouseMoved(x,y, mod)
		})
		.mouseOverAction_({|view, x, y| // mouse is up
			// must set view.acceptsMouseOver = true
			~mouseloc = Point(x,y);
		})
		.mouseDownAction_({ |view, x, y, mod, button|
			~mouseloc = Point(x,y);
			if (button==0, { down = true; this.mouseDown(x, y, mod, button) });
			if (button==1, { this.rightMouseDown(x, y, mod, button) });
		})
		.mouseUpAction_({ |view, x, y, mod, button|
			~mouseloc = Point(x,y);
			if (button==0, { down = false; this.mouseUp(x, y, mod, button) });
			if (button==1, { this.rightMouseUp(x, y, mod, button) });
		});

		win.front;

		{ while { run } {
			frame = frame + 1;
			canvas.refresh;
			gap.wait; }
		}.fork(AppClock)
	}

	close {	run = false }

	mouseDown {|x, y, mod, button|}

	mouseUp {|x, y, mod, button|}

	mouseMoved {|x, y|}

	rightMouseDown {|x, y, mod, button|}

	rightMouseUp {|x, y, mod, button|}

	keyDown { |char, modifiers, unicode, keycode, key|
		//[char, modifiers, unicode, keycode, key].postln;
		keypressed = key;
	}

	keyUp { |char, modifiers, unicode, keycode, key|
		//[char, modifiers, unicode, keycode, key].postln;
		keypressed = nil;
	}

	draw {}
}



IxiSimpleButton {
	*new {|parent, label, action|
		^super.new.init(parent, label, action);
	}

	init {|parent, label, action|
		var skin=GUI.skin;
		var font = GUI.font.new(*skin.fontSpecs);
		var w = (label.bounds(font).width + 10).max(18); // (optimalWidth + 10).max(minWidth?20)
		Button.new(parent, w@GUI.skin.buttonHeight)
		.states_([
			[label, Color.black, Color.grey(0.5, 0.2)]
		])
		.action_(action);
	}
}




IxiSelection {
	var main, selectables, <selected, winRect;
	var color, bgcolor, visible, rect, >lowGraphicsMode;

	*new {|main|
		^super.new.init(main);
	}

	init {|amain|
		main = amain;
		selectables = List.new;
		rect = Rect(0,0,0,0);
		color = Color(0, 0, 0, 0.4);
	}

	updateselectables {|ref| selectables = ref } // must know who can be selected

	start {|x,y|
		selectables = main.boxes;
		visible = true;
		rect = Rect(0,0,0,0);
		rect.left = x;
		rect.top = y;
		selectables.collect(_.deselect);// all just in case
		selected = List.new;
	}

	stop {|x,y|
		var actualrect = rect.deepCopy; // calculate the actual rect of the shape for contains

		selectables.collect(_.deselect);// clear all just in case
		selected = List.new;

		if (rect.width < 0, {
			actualrect.left = rect.left+rect.width;
			actualrect.width = rect.width.abs
		});
		if (rect.height < 0, {
			actualrect.top = rect.top+rect.height;
			actualrect.height = rect.height.abs
		});

		visible = false;
		selectables.do({|obj|
			if ( actualrect.contains(obj.rect.center)==true, { //contained
				selected.add(obj);
				obj.select;
			})
		});

		selected.do({|sel| sel.group(selected) });
		("selected"+selected.size+"items").postln;
	}

	draw {
		if (visible==true, {
			Pen.color = color;
			Pen.fillRect( rect );
		})
	}

	mouseDragged {|x,y|
		rect.width = x - rect.left;
		rect.height = y - rect.top;
	}

}






IxiBox {
	var <>rect, color, bgcolor, <size=17, offset, initcolor, >friends, visible=true;

	*new { |point|
		^super.new;//.init(point);
	}

	init {|point|
		if ( point.isNil, //point is rect.origin but we operate with left,top,w,h
			{ rect = Rect(~stagewidth/2, ~stageheight/2, size, size) - Rect(size/2,size/2,0,0) },
			{ rect = Rect(point.x, point.y, size, size) - Rect(size/2,size/2,0,0) });

		color = Color.black;
		initcolor = color;
		bgcolor = Color(1,1,1,0); //alpha 0

		friends = List.new;//set by Selection
	}

	select {
		friends = List.new; //clear, will be asigned after select()
		color = Color.red;
	}

	deselect {
		friends = List.new; //clear, will be asigned after select()
		color = initcolor;
	}

	group {|agroup|
		friends = agroup
	}

	draw {
		if (visible == true, {
			Pen.color = color;
			Pen.strokeRect( rect );
			Pen.color = bgcolor;
			Pen.fillRect(rect)
		})
	}

	mouseDown {|x,y|
		offset = [x-rect.left, y-rect.top, 0,0];
	}

	mouseUp {|x,y|
		offset = 0;
	}

	rightMouseDown {|x,y|

	}

	rightMouseUp {|x,y|

	}

	move {|delta|
		rect = rect + delta;
	}

	newrect {|x,y|
		^Rect(x, y, size, size) - offset;
	}

	dragged {|x,y| // mouseloc
		var oldr = rect;
		rect = this.newrect(x,y);
		friends.do({|friend|
			if (friend!=this, { // move friends with me (but not me!)
				friend.move(rect-oldr)
			})
		})
	}

	inside {|x,y|
		^rect.containsPoint(Point(x,y))
	}

	close {}
}