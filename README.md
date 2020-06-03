# feedback
Experiments on feedback (with supercollider) by ixi-audio.net. This project is part of the Ikersoinu research group at the Art and Technology department of the University of the Basque Country (EHU/UPV).

license GPL

check example.scd file for usage

Description:
Feedback1 is the main feedback system. The synthdef is based on https://sccode.org/1-U by Nathaniel Virgo

Some utilities:
- GNeck simulates a guitar neck to be able to set chords

- ChordsGUI allows to manually set each a 6 note chord

- AutoGUI allows to generate random values for slider controls in Feedback1

- Chords contains the MIDI values for standard guitar chords (asumming stardard tuning and normalised to 0)

- Effects: there are several effects that can be connected to the output chanel of the Feedback or INTO the loop channel of the Feedback (defaults to channel 10). Experiment with those different options.
Tremolo, Normalizer, Compresor/Expander, EQ, Limiter, AutoNotch (notches the main resonant frequency automatically)

![screenshot](/screenshot.jpg?raw=true "screenshot")


