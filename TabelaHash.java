/*
Classe que implementa a estrutura da Tabela Hash com encadeamento separado.
Gerencia a inserção, busca e as diferentes funções de hashing.
Mantém o cálculo de checksum e métricas de colisão.
 */
public class TabelaHash {

    public No[] tabela;
    public int m;
    public int tipoHash;
    
    public final int H_DIV = 0;
    public final int H_MUL = 1;
    public final int H_FOLD = 2;

    public long checksumSoma;
    public int checksumContador;

    /*
    Construtor da Tabela Hash.
    Inicializa o vetor de nós e define o tipo de hash a ser usado.
     */
    public TabelaHash(int tamanho, int tipo) {
        this.m = tamanho;
        this.tabela = new No[tamanho];
        this.tipoHash = tipo;
        this.checksumSoma = 0;
        this.checksumContador = 0;
    }

    /*
    Implementação da Função Hash por Divisão.
    h(k) = k mod m
     */
    public int hashDivisao(long k) {
        long resto = k % this.m;
        return (int) resto;
    }

    /*
    Implementação da Função Hash por Multiplicação.
    h(k) = floor(m * (k * A mod 1))
    Implementado manualmente sem uso de Math.
     */
    public int hashMultiplicacao(long k) {
        double A = 0.6180339887;
        double produto = k * A;
        double parteInteira = (long) produto;
        double fracao = produto - parteInteira;
        
        double resultado = this.m * fracao;
        return (int) resultado;
    }

    /*
    Implementação da Função Hash por Dobramento.
    Soma blocos de 3 dígitos da chave e aplica mod m.
     */
    public int hashDobramento(long k) {
        long soma = 0;
        long tempK = k;
        
        while (tempK > 0) {
            long bloco = tempK % 1000;
            soma = soma + bloco;
            tempK = tempK / 1000;
        }
        
        long resto = soma % this.m;
        return (int) resto;
    }

    /*
    Roteador que chama a função de hash apropriada conforme configuração.
    Realiza também a atualização do checksum para os primeiros 10 itens.
     */
    public int calcularHash(long k) {
        int indice = 0;
        
        if (this.tipoHash == H_DIV) {
            indice = hashDivisao(k);
        }
        if (this.tipoHash == H_MUL) {
            indice = hashMultiplicacao(k);
        }
        if (this.tipoHash == H_FOLD) {
            indice = hashDobramento(k);
        }

        if (this.checksumContador < 10) {
            this.checksumSoma = (this.checksumSoma + indice); 
            this.checksumContador = this.checksumContador + 1;
        }

        return indice;
    }

    /*
    Insere uma chave na tabela.
    Resolve colisões por encadeamento separado (lista manual).
    Retorna vetor com [colisoesNaTabela, colisoesNaLista].
     */
    public long[] inserir(long k) {
        int indice = calcularHash(k);
        
        long colisaoTabela = 0;
        long colisaoLista = 0;

        if (this.tabela[indice] == null) {
            this.tabela[indice] = new No(k);
        } else {
            colisaoTabela = 1;
            
            No atual = this.tabela[indice];
            while (atual.proximo != null) {
                colisaoLista = colisaoLista + 1;
                atual = atual.proximo;
            }
            colisaoLista = colisaoLista + 1; 
            atual.proximo = new No(k);
        }

        long[] resultado = new long[2];
        resultado[0] = colisaoTabela;
        resultado[1] = colisaoLista;
        return resultado;
    }

    /*
    Busca uma chave na tabela percorrendo a lista encadeada.
    Retorna o número de comparações positivo (sucesso) ou negativo (fracasso).
     */
    public long buscar(long k) {
        int indice = 0;
        if (this.tipoHash == H_DIV) indice = hashDivisao(k);
        if (this.tipoHash == H_MUL) indice = hashMultiplicacao(k);
        if (this.tipoHash == H_FOLD) indice = hashDobramento(k);

        long comparacoes = 0;
        No atual = this.tabela[indice];

        while (atual != null) {
            comparacoes = comparacoes + 1;
            if (atual.chave == k) {
                return comparacoes;
            }
            atual = atual.proximo;
        }

        return -comparacoes; 
    }

    /*
    Retorna o valor final do Checksum para auditoria.
     */
    public long getChecksum() {
        return this.checksumSoma % 1000003;
    }
}
