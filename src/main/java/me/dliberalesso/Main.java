package me.dliberalesso;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "mostra esta mensagem");
        options.addOption(Option.builder("m")
                .desc("inicia no modo (G)erenciador de processos ou "
                        + "inicia no modo (H)host e simula core(s) da CPU")
                .required(true)
                .hasArg(true)
                .argName("MODO")
                .build()
        );
        options.addOption(Option.builder("ip")
                .desc("indica o endereço IP para onde deve conectar")
                .hasArg(true)
                .argName("127.0.0.1")
                .build()
        );
        options.addOption(Option.builder("p")
                .desc("indica a PORTA por onde deve conectar")
                .hasArg(true)
                .argName("54321")
                .build()
        );
        options.addOption(Option.builder("c")
                .desc("indica por quantos CORES/CONEXÕES deve esperar")
                .hasArg(true)
                .type(Number.class)
                .argName("4")
                .build()
        );

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine line = parser.parse(options, args);
            System.out.println(line);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("TrabalhoSO2", options, true);
        }
    }
}
