package de.seppl.sebfinance;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import de.seppl.sebfinance.argument.ArgumentParser;
import de.seppl.sebfinance.argument.Arguments;
import de.seppl.sebfinance.argument.Arguments.EmptyFile;
import de.seppl.sebfinance.kontoauszug.Kontoauszug;
import de.seppl.sebfinance.kontoauszug.Lastschrift;
import de.seppl.sebfinance.pdf.ContentParserV1;
import de.seppl.sebfinance.pdf.ContentParserV2;
import de.seppl.sebfinance.pdf.KontoauszugParser;
import de.seppl.sebfinance.pdf.PdfParser;
import de.seppl.sebfinance.pdf.RawPdf;
import de.seppl.sebfinance.print.ConsolePrinter;
import de.seppl.sebfinance.print.Report;
import de.seppl.sebfinance.print.Report.ReportLine;
import de.seppl.sebfinance.print.ReportBuilder;


public class Main
{

    public static void main(String[] args)
    {
        Arguments arguments = new Arguments();
        ArgumentParser argsParser = new ArgumentParser(args);

        PdfParser pdfParser = new PdfParser();
        Collection<ContentParser> contentParsers = Arrays.asList(//
            new ContentParserV1(), //
            new ContentParserV2());

        ReportBuilder reportBuilder = new ReportBuilder();
        ConsolePrinter<ReportLine> consolePrinter = new ConsolePrinter<>("Reportlines",
            ReportLine.columns());

        Collection<File> pdfs = files(arguments, argsParser);

        Collection<Kontoauszug> auszuege = parse(pdfParser, contentParsers, pdfs);

        print(auszuege);

        Collection<Report> reports = reportBuilder.sollReports(auszuege);

        reports.stream().sorted().forEach(r -> {
            System.out.println(r.auszug().monat());
            consolePrinter.print(r.lines());

            r.posten(Lastschrift.SONSTIGES).forEach(p -> System.out.println(p));
            System.out.println("");
        });
    }

    private static Collection<File> files(Arguments arguments, ArgumentParser argsParser)
    {
        File pdf = argsParser.parse(arguments.pdf());

        if (!(pdf instanceof EmptyFile))
            return Arrays.asList(pdf);

        File dir = argsParser.parse(arguments.dir());

        if (dir instanceof EmptyFile)
            throw new IllegalArgumentException("No files found!");

        if (!dir.isDirectory())
            throw new IllegalArgumentException("No direcory:" + dir);

        return Arrays.asList(dir.listFiles());
    }

    private static Optional<Kontoauszug> parse(PdfParser pdfParser,
        Collection<KontoauszugParser> kontoauszugParsers, Collection<File> pdfs)
    {
        Collection<RawPdf> raws = pdfs.stream() //
            .map(pdfParser::raw) //
            .collect(Collectors.toList());

        kontoauszugParsers.stream() //
        .map(mapper)
        
        Collection<String> content = pdfParser.raw(pdfs);
        if (content.isEmpty())
        {
            System.err.println("Empty content: " + pdfs);
            return Optional.empty();
        }

        for (ContentParser contentParser : contentParsers)
        {
            try
            {
                return Optional.of(contentParser.kontoauszug(content));
            }
            catch (IllegalStateException e)
            {
                if (!e.getMessage().equals("Keine Posten"))
                    System.err.println(e.getMessage());
            }
        }
        System.err.println("No fitting ContentParser found!");
        return Optional.empty();
    }

    private static Kontoauszug kontoauszug(Collection<KontoauszugParser> kontoauszugParsers, RawPdf raw)
    {
        kontoauszugParsers.stream() //
            .map(p -> p.kontoauszug(rawPdf));

        return null;
    }
}
