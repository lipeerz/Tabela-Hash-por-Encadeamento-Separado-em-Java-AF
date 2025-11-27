/*
Classe simples para o nó da lista encadeada.
Armazena a chave e a referência para o próximo nó.
 */
public class No {
    public long chave;
    public No proximo;

    /*
  Construtor do Nó.
     */
    public No(long chave) {
        this.chave = chave;
        this.proximo = null;
    }
}
