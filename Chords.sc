
GuitarChords {
	classvar <chords;
	// [ 0, 5, 10, 15, 19, 24 ]
	*initClass {
		chords  = Dictionary.new;
		chords[\AM] = [ 0, 5, 12, 17, 21, 24 ]; //[40, 45, 52, 57, 61, 64]-40;
		chords[\Am] = [ 0, 5, 12, 17, 20, 24 ];
		chords[\A7] = [ 0, 5, 12, 15, 21, 24 ];
		chords[\Am7] = [ 0, 5, 12, 15, 20, 24 ];
		chords[\AM7] = [ 0, 5, 12, 16, 21, 24 ];

		chords[\BM] = [ 2, 7, 14, 19, 23, 26 ];
		chords[\Bm] = [ 2, 7, 14, 19, 22, 26 ];
		chords[\B7] = [ 0, 7, 11, 17, 19, 26 ]; //X1
		chords[\Bm7] = [ 2, 7, 14, 17, 22, 26 ];
		chords[\BM7] = [ 2, 7, 14, 18, 23, 26 ];

		chords[\CM] = [ 0, 8, 12, 15, 20, 24 ];
		chords[\Cm] = [ 3, 8, 15, 20, 23, 27 ];
		chords[\C7] = [ 0, 8, 12, 18, 20, 24 ];
		chords[\Cm7] = [ 3, 8, 15, 18, 23, 27 ];
		chords[\CM7] = [ 0, 8, 12, 15, 19, 24 ];


		chords[\DM] = [ 0, 5, 10, 17, 22, 26 ]; //X1 all
		chords[\Dm] = [ 0, 5, 10, 17, 22, 25 ];
		chords[\D7] = [ 0, 5, 10, 17, 20, 26 ];
		chords[\Dm7] = [ 0, 5, 10, 17, 20, 25 ];
		chords[\DM7] = [ 0, 5, 10, 17, 21, 26];


		chords[\EM] = [ 0, 7, 12, 16, 19, 24 ];
		chords[\Em] = [ 0, 7, 12, 15, 19, 24 ];
		chords[\E7] = [ 0, 7, 10, 16, 19, 24 ];
		chords[\Em7] = [ 0, 7, 10, 15, 19, 24 ];
		chords[\EM7] = [ 0, 7, 12, 19, 23, 28 ];

		chords[\FM] = [ 1, 8, 13, 17, 20, 25 ];
		chords[\Fm] = [ 1, 8, 13, 16, 20, 25 ];
		chords[\F7] = [ 1, 8, 11, 17, 20, 25 ];
		chords[\Fm7] = [ 1, 8, 11, 16, 20, 25 ];
		chords[\FM7] = [ 0, 5, 13, 17, 20, 24 ]; //X1

		chords[\GM] = [ 3, 7, 10, 15, 19, 27 ];
		chords[\Gm] = [ 3, 10, 15, 18, 22, 27 ];
		chords[\G7] = [ 3, 7, 10, 15, 19, 25 ];
		chords[\Gm7] = [ 3, 10, 13, 18, 22, 27 ];
		chords[\GM7] = [ 0, 5, 15, 19, 22, 26 ]; //X1 X2
	}

/*	*a {
		^chords.select{ |item| item.key.asString.contains("A") };
	}*/

	// major {
	// 	chords.select{ |item| item.key.asString.contains("M") };
	// }
}

