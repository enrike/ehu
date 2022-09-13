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
CompressorGUI.new;
FreqShiftGUI.new;
LimiterGUI.new;
ChaosPitchShiftGUI.new;
PitchShiftGUI.new;
GainLimiterGUI.new;
DelayGUI.new;
FreeverbGUI.new;
GVerbGUI.new;
M2stGUI.new; // takes a mono channel and duplicates it in two channels
Shooter.new(filepath:"/path/to/folder/with/files") // shots a sample when onset is detected. (max 5 layers at the same time)
Mirror.new; // reverses incoming onsets
PatcherGUI.new // patches the audio from a bus into another bus

Launcher.new; // simple GUI to launch all effects

ChannelEQ.new; // download and install from https://github.com/enrike/supercollider-channeleq requieres wslib and tabbed views quarks
ChannelEQ.new(10); // you can use the loop bus value to control the internal feedback EQ

BufferPlayerGUI.new // play a sound file
Pads.new("/path/to/files/*.flac") // trigger samples from a folder
// Pads arguments are path, out=0, col=5, size=120, amp=1, midif=1, loop=0, tail=0, nk2


// PRESET SYSTEM.
/* the save/open buttons allow to save and load the current state of the window and GUI inside the "presets" directory.
Save will name the preset like EFFECTNAME_yearmothday_hourminsecs.preset
When the window opens it tries to load a prest named "presets/EFFECTNAME_default.preset"
they can be loaded like this too:
*/

CompanderGUI.new(preset:"201024_182949"); // will load "presets/compander_201024_182949.preset"
GainLimiterGUI.new(preset:"201024_160150"); // will load "presets/gainlimiter_201024_160150.preset". Beware the missing capitals

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

// AUTO system and auto presets
/* auto button just opens a window that allows to set each slider to a rand variable every N secs. Presets can be saved and also set on initialization. This below should open Feedback and its auto window with the default auto preset
*/
Feedback1.new(autopreset:"default");


// save ALL open window's current state at the same time
~ehuws.collect(_.save)
// because each wind add itsleft to ~ehuws list


