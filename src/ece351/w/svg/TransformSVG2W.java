/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.w.svg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// import org.apache.tools.ant.taskdefs.Transform;
import org.parboiled.common.ImmutableList;

import ece351.w.ast.WProgram;
import ece351.w.ast.Waveform;


public final class TransformSVG2W {
	
	/**
	 * Transforms an instance of WSVG to an instance of WProgram.
	 * Write this algorithm in whatever way you wish.
	 * Remember that the AST is immutable.
	 * You might want to build up some mutable temporary structures.
	 * ImmutableList can be used as a "mutable" temporary structure if the 
	 * local variable is not final: just re-assign the local variable to the new list.
	 * 
	 * We used to give more detailed comments on the staff algorithm,
	 * but many students in several offerings of this course found
	 * those comments confusing, and asked for them to be removed.
	 * 
	 * @see #COMPARE_Y_X 
	 * @see #transformLinesToWaveform(List, List)
	 * @see java.util.ArrayList
	 * @see java.util.LinkedHashSet
	 */
	public static final WProgram transform(final PinsLines pinslines) {
		final List<Line> lines = new ArrayList<Line>(pinslines.segments);
		final List<Pin> pins = new ArrayList<Pin>(pinslines.pins);
		ImmutableList<Waveform> waveforms = ImmutableList.of(); // empty
		Collections.sort(lines, COMPARE_Y_X); // should make the search for line range faster
		Collections.sort(pins, COMPARE_Y_PIN); // sort pins by y value
		// assume my own svg generation
		// linear search
		for(Pin p:pins){ // should be least to greatest sorted
			// make a waveform for this pin
			int y_mid = p.y;
			List<Line> le_lines = new ArrayList<>(); // lines per pin
			for( Line l : lines){
				if((Math.abs(l.y1 - y_mid) <= 50 ) && (l.x1 != l.x2) && (l.y1 == l.y2)){ // ignore vertical lines
					le_lines.add(l); // line that is within range
				}
			}
			waveforms = waveforms.append(transformLinesToWaveform(le_lines, p));
			} //  O(lp) algorithm, could have been sped up to O(l + p) by simple interating over lines once with an iterator since its sorted
		return new WProgram(waveforms);
	}

	/**
	 * Transform a list of Line to an instance of Waveform.
	 * The concept of a y-midpoint might be useful: 1 is a line above; 0 is a line below.
	 * What to do about "dots"?
	 * ImmutableList can be used as a "mutable" temporary structure if the 
	 * local variable is not final: just re-assign the local variable to the new list.
	 * 
	 * We used to give more detailed comments on the staff algorithm,
	 * but many students in several offerings of this course found
	 * those comments confusing, and asked for them to be removed.
	 * 
	 * @see #COMPARE_X
	 * @see #transform(PinsLines)
	 * @see Pin#id
	 */
	private static Waveform transformLinesToWaveform(final List<Line> lines, final Pin pin) {
		if(lines.isEmpty()) return null;
		Waveform wave = new Waveform(pin.id);
		int y_mid = pin.y;
		Collections.sort(lines, COMPARE_X); // sort from left to right
		for( Line l : lines){
				// check what line it is
				if(l.y1 > y_mid) { // line below
					wave = wave.append("0");
				}
				else if (l.y1 < y_mid){ // line above
					wave = wave.append("1");
				}
		}
		return wave;
	}

	/**
	 * Sort a list of lines according to their x position.
	 * 
	 * @see java.util.Comparator
	 */
	public final static Comparator<Line> COMPARE_X = new Comparator<Line>() {
		@Override
		public int compare(final Line l1, final Line l2) {
			if(l1.x1 < l2.x1) return -1;
			if(l1.x1 > l2.x1) return 1;
			if(l1.x2 < l2.x2) return -1;
			if(l1.x2 > l2.x2) return 1;
			return 0;
		}
	};

	public final static Comparator<Pin> COMPARE_Y_PIN = new Comparator<Pin>() {
		@Override
		public int compare(final Pin p1, final Pin p2) {
			if(p1.y < p2.y) return -1;
			if(p1.y > p2.y) return 1;
			return 0;
		}
	};

	/**
	 * Sort a list of lines according to their y position first, and then x position second.
	 * 
	 * @see java.util.Comparator
	 */
	public final static Comparator<Line> COMPARE_Y_X = new Comparator<Line>() {
		@Override
		public int compare(final Line l1, final Line l2) {
			final double y_mid1 = (double) (l1.y1 + l1.y2) / 2.0f;
			final double y_mid2 = (double) (l2.y1 + l2.y2) / 2.0f;
			final double x_mid1 = (double) (l1.x1 + l1.x2) / 2.0f;
			final double x_mid2 = (double) (l2.x1 + l2.x2) / 2.0f;
			if (y_mid1 < y_mid2) return -1;
			if (y_mid1 > y_mid2) return 1;
			if (x_mid1 < x_mid2) return -1;
			if (x_mid1 > x_mid2) return 1;
			return 0;
		}
	};

}
