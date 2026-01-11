import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class CircleCreate2 {

	private static final String FILE_NAME = "tonleitern-byjava2.svg";
	
	private static PrintWriter out;

	public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException {
		out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FILE_NAME), "UTF-8"));
		Locale.setDefault(Locale.ENGLISH);

		// Outer circle
		double ocr = 214;
		double ocx = 389.7;
		double ocy = 326.5;
		double ocy2 = 793.5;

		double icr = 10; // inner hole

		// inner circles
		double ncr = ocr * 0.13;
		double ncrMM = ocr * 0.14;
		double ncPosR = ocr * 0.80;

		// indicator
		double idPosR = ocr * 0.55;
		double idWidth = ocr / 9 * 1.5;

		Position[] pos = { //
				new Position("C", true, "0"), //
				new Position("C♯", false, "7♯"), //
				new Position("D♭", false, "5♭"), //
				new Position("D", true, "2♯"), //
				new Position("D♯", false, ""), //
				new Position("E♭", false, "3♭"), //
				new Position("E", true, "4♯"), //
				new Position("E♯,F♭", false, ""), //
				new Position("F", true, "1♭"), //
				new Position("F♯", false, "6♯"), //
				new Position("G♭", false, ""), //
				new Position("G", true, "1♯"), //
				new Position("G♯", false, ""), //
				new Position("A♭", false, "4♭"), //
				new Position("A", true, "3♯"), //
				new Position("A♯", false, ""), //
				new Position("B", false, "2♭"), //
				new Position("H", true, "5♯"), //
				new Position("H♯,C♭", false, "") //
		};

		int majorPos = 0;
		int minorPos = 14;

		out.println("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
		out.printf("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">%n");
		out.printf("   <g font-size=\"30\" font=\"sans-serif\" fill=\"black\" stroke=\"none\" text-anchor=\"middle\">%n");

		// bottom circle (upper on page)
		out.printf("<g transform=\"translate(%f %f)\">%n", ocx, ocy);
		out.printf("   <circle r=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\" />%n", ocr);
		out.printf("   <circle r=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\" />%n", icr);
		cross(icr);
		for (int i = 0; i < 19; i++) {
			double angle = 360d / 19 * i;
			out.printf("   <g transform=\"rotate(%f 0 0) translate(0 %f)\">%n", angle, -ncPosR);
			out.printf("      <circle r=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\" />%n", ncr);
			out.printf("      <text y=\"10\" fill=\"black\">%s</text>%n", pos[i].note);
			out.printf("      <text y=\"60\" fill=\"black\">%s</text>%n", pos[i].sharpflat);
			out.printf("   </g>%n", icr);
		}
		out.printf("</g>%n");

		// top circle (lower on page)
		out.printf("<g transform=\"translate(%f %f)\">%n", ocx, ocy2);
		out.printf("   <circle r=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\" />%n", ocr);
		out.printf("   <circle r=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\" />%n", icr);
		cross(icr);
		for (int i = 0; i < 19; i++) {
			if (pos[i].hole) {
				double angle = 360d / 19 * i;
				out.printf("   <g transform=\"rotate(%f 0 0) translate(0 %f)\">%n", angle, -ncPosR);
				if (i == majorPos) {
					out.printf("      <circle r=\"%f\" stroke=\"blue\" stroke-width=\"8\" fill=\"white\" />%n", ncrMM);
				}
				if (i == minorPos) {
					out.printf("      <circle r=\"%f\" stroke=\"red\" stroke-width=\"8\" fill=\"white\" />%n", ncrMM);
				}
				out.printf("      <circle r=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\" />%n", ncr);
				cross(icr);
				out.printf("   </g>%n", icr);
			}
		}
		double idofsx = -idWidth / 2;
		double idofsy = -idPosR - idWidth / 2;
		out.printf(
				"   <rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" stroke=\"black\" stroke-width=\"2\" fill=\"white\"/>%n",
				idofsx, idofsy, idWidth, idWidth);
		out.printf("</g>%n");
		out.printf("</g>%n");
		out.printf("</svg>%n");
		out.close();
		System.out.println("SVG written to file '"+FILE_NAME +"'.");
	}

	public static void cross(double r) {

		double x1 = -r / 2;
		double y1 = -r / 2;
		double x2 = r / 2;
		double y2 = r / 2;
		out.printf("   <line x1=\"0\" y1=\"%f\" x2=\"0\" y2=\"%f\" stroke=\"black\" stroke-width=\"2\" />%n", y1, y2);
		out.printf("   <line x1=\"%f\" y1=\"0\" x2=\"%f\" y2=\"0\" stroke=\"black\" stroke-width=\"2\" />%n", x1, x2);
	}

	static class Position {
		String note;
		boolean hole;
		String sharpflat;

		public Position(String note, boolean hole, String sharpflat) {
			super();
			this.note = note;
			this.hole = hole;
			this.sharpflat = sharpflat;
		}
	}

}