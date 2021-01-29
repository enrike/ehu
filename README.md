# ehu
Experimental tools to make music based in supercollider by ixi-audio.net under the Ikersoinu research group at the Art and Technology department of the University of the Basque Country (EHU/UPV).

This project grew around the Feedback1.sc system.

license GPL

check example.scd file for usage

Description:
- Feedback1 is the a feedback system. The synthdef is based on https://sccode.org/1-U by Nathaniel Virgo

- GNeck simulates a guitar neck to be able to set chords

- ChordsGUI allows to manually set each a 6 note chord

- AutoGUI allows to generate random values for slider controls in Feedback1

- Chords contains the MIDI values for standard guitar chords (asumming stardard tuning and normalised to 0)

- Effects: there are several simple effects that can be connected to the output chanel of the Feedback or INTO the loop channel of the Feedback (defaults to channel 10). Experiment with those different options.
Tremolo, Normalizer, Compresor/Expander, EQ, Limiter, AutoNotch (notches the main resonant frequency automatically), etc...

There are also tools used to construct the GUIs (EffectsGUI.sc, AutoGUI.sc, ParamWinGUI.sc)

![screenshot](/screenshot.jpg?raw=true "screenshot")


