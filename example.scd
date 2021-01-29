/* FEEDBACK
*/
f = Feedback1.new;

/* EFFECTS
// effects I use in combination with the feedback unit. some are just quick tests
*/
AutoNotchGUI.new;
TremoloGUI.new;
NormalizerGUI.new;
CompanderGUI.new;
FreqShiftGUI.new
LimiterGUI.new
ChaosPitchShiftGUI.new
PitchShiftGUI.new;
GainLimiterGUI.new;

Launcher.new; // simple GUI to launch all effects

ChannelEQ.new; // download and install from https://github.com/enrike/supercollider-channeleq requieres wslib and tabbed views quarks
ChannelEQ.new(10); // you can use the loop bus value to control the internal feedback EQ

BufferPlayerGUI.new




// PRESET SYSTEM.
/* the save/open buttons allow to save and load the current state of the window and GUI inside the "presets" directory.
Save will name the preset like EFFECTNAME_yearmothday_hourminsecs.preset
When the window opens it tries to load a prest named "presets/EFFECTNAME_default.preset"
they can be loaded like this too
*/
CompanderGUI.new(preset:"201024_182949"); // will load "presets/compander_201024_182949.preset"
GainLimiterGUI.new(preset:"201024_160150"); // will load "presets/gainlimiter_201024_160150.preset"



// presets + defer
( // if you use defer you must pass the ref to nowExecutingPath to find the presets folder
x = thisProcess.nowExecutingPath.dirname;
{
	CompanderGUI.new(exepath:x, preset:"201024_182949"); //make sure the presets are there
	AutoNotchGUI.new(exepath:x, preset:"201026_122236");
	ChannelEQ.new;
	GainLimiterGUI.new(exepath:x, preset:"201026_122001");
}.defer(3); // for instance
)


