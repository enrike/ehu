(
f = Feedback1.new;
f.chord([0, 6.1, 10, 15.2, 22, 24 ]);
f.gui;
//f.midi; // setup
//f.nanok; // connect to nanokontrol2
f.audio;


// other utils I use
s.waitForBoot({
	ChannelEQ.new;
	CompanderGUI.new;
	s.meter(2,10);
})
)


