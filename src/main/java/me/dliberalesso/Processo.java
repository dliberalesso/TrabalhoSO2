package me.dliberalesso;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Processo implements Serializable {
    public static final String APTO = "Apto";
    public static final String EXEC = "Executando";

    private int pid, tempo;
    private String nome, tamanho, criacao, execucao, estado, host;

    public Processo(int pid, String nome, int tempo, String tamanho) {
        this.pid = pid;
        this.nome = nome;
        this.tempo = tempo;
        this.tamanho = tamanho;

        this.criacao = agora();
        this.execucao = null;
        this.estado = APTO;
        this.host = null;
    }

    public int getPid() {
        return pid;
    }

    public String getNome() {
        return nome;
    }

    public int getTempo() {
        return tempo;
    }

    public void setExecucao(String execucao) {
        this.execucao = execucao;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public static String agora() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format(
                " %3d | %4s | %7s | %8s | %8s | %10s | %4s",
                pid, nome, tamanho, criacao, execucao, estado, host
        );
    }

    public String toLog() {
        return String.format(
                " %3d | %4s | %7s | %8s | %8s | %4s",
                pid, nome, tamanho, criacao, execucao, host
        );
    }
}
