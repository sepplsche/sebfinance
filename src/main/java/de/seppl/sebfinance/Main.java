package de.seppl.sebfinance;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

import org.apache.commons.lang.StringUtils;

import de.seppl.sebfinance.argument.ArgumentParser;
import de.seppl.sebfinance.argument.Arguments;
import de.seppl.sebfinance.kontoauszug.Kontoauszug;
import de.seppl.sebfinance.kontoauszug.Lastschrift;
import de.seppl.sebfinance.kontoauszug.Posten;
import de.seppl.sebfinance.pdf.ContentParser;
import de.seppl.sebfinance.pdf.ContentParserV1;
import de.seppl.sebfinance.pdf.ContentParserV2;
import de.seppl.sebfinance.pdf.KontoauszugParser;
import de.seppl.sebfinance.pdf.PdfParser;
import de.seppl.sebfinance.print.ConsolePrinter;
import de.seppl.sebfinance.print.PrintableColumn;

import static de.seppl.sebfinance.print.PrintableColumn.left;
import static de.seppl.sebfinance.print.PrintableColumn.right;

public class Main {
    public static void main(String[] args) {
        System.out.println(String.format("parsing arguments '%s'...", StringUtils.join(args, " ")));
        Arguments arguments = new Arguments();
        ArgumentParser argsParser = new ArgumentParser(args);
        Collection<File> pdfs = argsParser.parse(arguments.files());
        LocalDate date = argsParser.parse(arguments.date());

        System.out.println(String.format("parsing %s PDFs...", pdfs.size()));
        Collection<Kontoauszug> auszuege = parse(pdfs, date);

        System.out.println(String.format("printing %s Kontoauszüge...", auszuege.size()));
        printAuszuege(auszuege);
    }

    private static Collection<Kontoauszug> parse(Collection<File> pdfs, LocalDate date) {
        PdfParser pdfParser = new PdfParser();
        Collection<ContentParser> contentParsers = Arrays.asList(//
                new ContentParserV1(), //
                new ContentParserV2());
        KontoauszugParser kontoauszugParser = new KontoauszugParser(contentParsers, date);

        return pdfs.stream() //
                .filter(pdf -> pdf.getName().startsWith("rep")) //
                .map(pdfParser::raw) //
                .map(kontoauszugParser::kontoauszug) //
                .collect(Collectors.toList());
    }

    private static void printAuszuege(Collection<Kontoauszug> auszuege) {
        Collection<PrintableColumn<Posten>> columns =
                Arrays.asList(right("Betrag", e -> (e.gutschrift() ? "+" : "-") + e.betrag() + " CHF"), //
                        left("Kategorie", e -> e.kategorie().name()), //
                        left("Beschreibung", e -> e.kategorie().equals(Lastschrift.SONSTIGES) ? e.verwendung() : ""));

        ConsolePrinter<Posten> printer = new ConsolePrinter<>(columns);

        auszuege.stream().sorted().forEach(auszug -> {
            System.out.println(String.format("%s Posten für '%s':", auszug.posten().size(), auszug.monat()));

            Collection<Posten> posten = auszug.posten().stream() //
                    .sorted((a, b) -> compare(a, b)) //
                    .collect(toList());

            printer.print(posten);
        });
    }

    private static int compare(Posten a, Posten b) {
        int compKatClass = a.kategorie().getClass().getSimpleName().compareTo(b.kategorie().getClass().getSimpleName());
        if (compKatClass != 0)
            return compKatClass;
        int compKat = a.kategorie().name().compareTo(b.kategorie().name());
        if (compKat != 0)
            return compKat;
        return a.betrag() - b.betrag();
    }
}
