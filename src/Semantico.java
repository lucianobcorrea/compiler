import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class Semantico {

	// Lista de tokens
	static final int T_ALGORITMO         =   1;
	static final int T_FIM_ALGORITMO     =   2;
	static final int T_DEF_VARIAVEIS     =   3;
	static final int T_VIRGULA           =   4;
	static final int T_FIM_LINHA         =   5;
	static final int T_SE                =   6;
	static final int T_SENAO             =   7;
	static final int T_FIM_SE            =   8;
	static final int T_ENQUANTO          =   9;
	static final int T_FIM_ENQUANTO      =  10;
	static final int T_PARA              =  11;
	static final int T_SETA              =  12;
	static final int T_ATE               =  13;
	static final int T_FIM_PARA          =  14;
	static final int T_LER               =  15;
	static final int T_ABRE_PAR          =  16;
	static final int T_FECHA_PAR         =  17;
	static final int T_ESCREVER          =  18;
	static final int T_MAIOR             =  19;
	static final int T_MENOR             =  20;
	static final int T_MAIOR_IGUAL       =  21;
	static final int T_MENOR_IGUAL       =  22;
	static final int T_IGUAL             =  23;
	static final int T_DIFERENTE         =  24;
	static final int T_MAIS              =  25;
	static final int T_MENOS             =  26;
	static final int T_VEZES             =  27;
	static final int T_DIVIDIDO          =  28;
	static final int T_RESTO             =  29;
	static final int T_ELEVADO           =  30;
	static final int T_NUMERO            =  31;
	static final int T_IDENTIFICADOR     =  32;
	static final int T_PONTO_VIRGULA     =  33;
	static final int T_ABRE_CHAVES       =  34;
	static final int T_FECHA_CHAVES      =  35;

	static final int T_FIM_FONTE         =  90;
	static final int T_ERRO_LEX          =  98;
	static final int T_NULO              =  99;

	static final int FIM_ARQUIVO         =  26;

  static final int E_SEM_ERROS       =   0;
  static final int E_ERRO_LEXICO     =   1;
  static final int E_ERRO_SINTATICO  =   2;
  static final int E_ERRO_SEMANTICO  =   3;

  // Variaveis que surgem no Lexico
  static File arqFonte;
  static BufferedReader rdFonte;
  static File arqDestino;
  static char   lookAhead;
  static int    token;
  static String lexema;
  static int    ponteiro;
  static String linhaFonte;
  static int    linhaAtual;
  static int    colunaAtual;
  static String mensagemDeErro;
  static StringBuffer tokensIdentificados = new StringBuffer();

  // Variaveis adicionadas para o sintatico
  static StringBuffer 	regrasReconhecidas = new StringBuffer();
  static int 			estadoCompilacao;
  
  // Variaveis adicionadas para o semantico
  static String 		    ultimoLexema; // criada para poder usar no codigo
                                          // guardar o lexema anterior
  static StringBuffer       codigoPython = new StringBuffer();
  static int 			    nivelIdentacao = 0; // para saber quantos espaços eu dou
  static NodoPilhaSemantica nodo_1;
  static NodoPilhaSemantica nodo_2;
  static NodoPilhaSemantica nodo_3;
  static PilhaSemantica     pilhaSemantica = new PilhaSemantica();
  static HashMap<String,Integer> tabelaSimbolos = new HashMap<String,Integer>();  

  public static void main( String s[] )
  {
      try {
          abreArquivo();
          abreDestino();
          linhaAtual     = 0;
          colunaAtual    = 0;
          ponteiro       = 0;
          linhaFonte     = "";
          token          = T_NULO;
          mensagemDeErro = "";
          tokensIdentificados.append( "Tokens reconhecidos: \n\n" );
          regrasReconhecidas.append( "\n\nRegras reconhecidas: \n\n" );
          estadoCompilacao 	= E_SEM_ERROS;

          // posiciono no primeiro token
          movelookAhead();
          buscaProximoToken();

          analiseSintatica();

          exibeSaida();

          gravaSaida( arqDestino );

          fechaFonte();

      } catch( FileNotFoundException fnfe ) {
          JOptionPane.showMessageDialog( null, "Arquivo nao existe!", "FileNotFoundException!", JOptionPane.ERROR_MESSAGE );
      } catch( UnsupportedEncodingException uee ) {
          JOptionPane.showMessageDialog( null, "Erro desconhecido", "UnsupportedEncodingException!", JOptionPane.ERROR_MESSAGE );
      } catch( IOException ioe ) {
          JOptionPane.showMessageDialog( null, "Erro de io: " + ioe.getMessage(), "IOException!", JOptionPane.ERROR_MESSAGE );
      } catch( ErroLexicoException ele ) {
          JOptionPane.showMessageDialog( null, ele.getMessage(), "Erro Lexico Exception!", JOptionPane.ERROR_MESSAGE );
      } catch( ErroSintaticoException ese ) {
          JOptionPane.showMessageDialog( null, ese.getMessage(), "Erro Sint�tico Exception!", JOptionPane.ERROR_MESSAGE );
      } catch( ErroSemanticoException esme ) {
          JOptionPane.showMessageDialog( null, esme.getMessage(), "Erro Semantico Exception!", JOptionPane.ERROR_MESSAGE );          
      } finally {
          System.out.println( "Execucao terminada!" );
      }
  }

  static void analiseSintatica() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {

      g();

      if ( estadoCompilacao == E_ERRO_LEXICO ) {
          JOptionPane.showMessageDialog( null, mensagemDeErro, "Erro Lexico!", JOptionPane.ERROR_MESSAGE );
      } else if ( estadoCompilacao == E_ERRO_SINTATICO ) {
          JOptionPane.showMessageDialog( null, mensagemDeErro, "Erro Sintatico!", JOptionPane.ERROR_MESSAGE );
      } else if ( estadoCompilacao == E_ERRO_SEMANTICO ) {
          JOptionPane.showMessageDialog( null, mensagemDeErro, "Erro Semantico!", JOptionPane.ERROR_MESSAGE );
      } else {
          JOptionPane.showMessageDialog( null, "Analise Sintatica terminada sem erros", "Analise Sintatica terminada!", JOptionPane.INFORMATION_MESSAGE );
		  acumulaRegraSintaticaReconhecida( "<G>" );
      }
  }

	// <G> ::= 'PROGRAMA' <LISTA> <CMDS> 'FIM'
	private static void g() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		if (token == T_ALGORITMO) {
			buscaProximoToken();

			String nomePrograma = lerNomePrograma();

			if(nomePrograma == null) {
				registraErroSintatico("Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual
						+ "\nErro: <" + linhaFonte + ">\nNome do programa esperado, mas encontrei: " + lexema);
			}

			lista();
			cmds();

            if(token == T_VIRGULA) {
                buscaProximoToken();
                if(token == T_NUMERO) {
                    buscaProximoToken();
                    if(token == T_PONTO_VIRGULA) {
                        buscaProximoToken();
                        cmds();
                    }
                }
            }


			if (token == T_FIM_ALGORITMO) {
				buscaProximoToken();
				acumulaRegraSintaticaReconhecida("<G> ::= 'PROGRAMA' <LISTA> <CMDS> 'FIM'");
			} else {
				registraErroSintatico("Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual
						+ "\nErro: <" + linhaFonte + ">\n('fim') esperado, mas encontrei: " + lexema);
			}
		} else {
			registraErroSintatico("Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual + "\nErro: <"
					+ linhaFonte + ">\n('programa') esperado, mas encontrei: " + lexema);
		}
	}

	// Método para ler o nome do programa
	private static String lerNomePrograma() throws IOException, ErroLexicoException {
		if (token == T_IDENTIFICADOR) {
			String nomePrograma = lexema;
			buscaProximoToken();
			return nomePrograma;
		} else {
			return null;
		}
	}

	// <LISTA> ::= 'VARIAVEIS' <VARS>
	private static void lista() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		if ( token == T_DEF_VARIAVEIS ) {
			buscaProximoToken();
			vars();
			if ( token == T_PONTO_VIRGULA ) {
				buscaProximoToken();
				acumulaRegraSintaticaReconhecida( "<LISTA> ::= 'VARIAVEIS' <VARS>" );
			} else {
				registraErroSintatico( "Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual + "\nErro: <" + linhaFonte + ">\n';' esperado, mas encontrei: " + lexema );
			}
		} else {
			registraErroSintatico( "Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual + "\nErro: <" + linhaFonte + ">\n('variaveis') esperado, mas encontrei: " + lexema );
		}
	}

  // <VARS> ::= <VAR> , <VARS> | <VAR>
  private static void vars() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  varDecl();
	  while ( token == T_VIRGULA ) {
		  buscaProximoToken();
		  varDecl();
	  }
	  acumulaRegraSintaticaReconhecida( "<VARS> ::= <VAR> , <VARS> | <VAR>" );
  }
  
  // <VAR_DECL> ::= <ID> 
  private static void varDecl() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
      id();
      regraSemantica( 2 );
	  acumulaRegraSintaticaReconhecida( "<VAR_DECL> ::= <ID>" );
  }

  // <VAR> ::= <ID> 
  private static void var() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
      id();
      regraSemantica( 4 );
	  acumulaRegraSintaticaReconhecida( "<VAR> ::= <ID>" );
  }

	// <ID> ::= [A-Z]+([A-Z]_[0-9])*
	private static void id() throws IOException, ErroLexicoException, ErroSintaticoException {
		if ( token == T_IDENTIFICADOR ) {
            buscaProximoToken();
            acumulaRegraSintaticaReconhecida("<ID> ::= [A-Z]+([A-Z]_[0-9])*");
        }else {
			registraErroSintatico( "Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual + "\nErro: <" + linhaFonte + ">\nEsperava um identificador. Encontrei: " + lexema );
		}
	}
   
  // <CMDS> ::= <CMD> ; <CMDS> | <CMD>
  private static void cmds() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  cmd();
	  while ( token == T_PONTO_VIRGULA ) {
		  buscaProximoToken();
		  cmd();
	  } 
	  acumulaRegraSintaticaReconhecida( "<CMDS> ::= <CMD> ; <CMDS> | <CMD>" );
  }

	// <CMD> ::= <CMD_SE>
	// <CMD> ::= <CMD_ENQUANTO>
	// <CMD> ::= <CMD_PARA>
	// <CMD> ::= <CMD_ATRIBUICAO>
	// <CMD> ::= <CMD_LER>
	// <CMD> ::= <CMD_ESCREVER>
	private static void cmd() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {

		switch ( token ) {
			case T_SE: cmd_se(); break;
			case T_ENQUANTO: cmd_enquanto(); break;
			case T_PARA: cmd_para(); break;
			case T_IDENTIFICADOR: cmd_atribuicao(); break;
			case T_LER: cmd_ler(); break;
			case T_ESCREVER: cmd_escrever(); break;
			case T_FECHA_CHAVES: cmd_se(); break;
			case T_FECHA_PAR: cmd_enquanto(); break;
			default:
				registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\nComando nao identificado va aprender a programar pois encontrei: " + lexema );
		}
		acumulaRegraSintaticaReconhecida( "<CMD> ::= <CMD_SE>|<CMD_ENQUANTO>|<CMD_PARA>|<CMD_ATRIBUICAO>|<CMD_LER>|<CMD_ESCREVER>" );
	}

	// <CMD_SE> ::= 'SE' <CONDICAO> <CMDS> ( 'FIM_SE'|'SENAO' <CMDS> 'FIM_SE' )
	private static void cmd_se() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		if (token == T_SE) {
			buscaProximoToken();
			if (token == T_ABRE_PAR) {
				buscaProximoToken();
				condicao();
				regraSemantica(28);
				if (token == T_FECHA_PAR) {
					buscaProximoToken();

					if (token == T_ABRE_CHAVES) {
						buscaProximoToken();
						cmds();
					} else {
						registraErroSintatico("Erro: '{' esperado mas encontrei: " + lexema);
					}

					if(token == T_FECHA_CHAVES) {
						buscaProximoToken();
					}

					if (token == T_SENAO) {
						buscaProximoToken();
						regraSemantica(29);
						if (token == T_ABRE_CHAVES) {
							buscaProximoToken();
							cmds();
							if (token == T_FECHA_CHAVES) {
								buscaProximoToken();
								regraSemantica(30);
								cmds();
							} else {
								registraErroSintatico("Erro: '}' esperado mas encontrei: " + lexema);
							}
						} else {
							registraErroSintatico("Erro: '{' esperado mas encontrei: " + lexema);
						}
					}
				} else {
					registraErroSintatico("Erro: ')' esperado mas encontrei: " + lexema);
				}
			} else {
				registraErroSintatico("Erro: '(' esperado mas encontrei: " + lexema);
			}
		}
		acumulaRegraSintaticaReconhecida("<CMD_SE> ::= 'SE' <CONDICAO> <CMDS> ( 'FIM_SE'|'SENAO' <CMDS> 'FIM_SE' )");
	}

	// <CMD_ENQUANTO> ::= 'ENQUANTO' <CONDICAO> <CMDS> 'FIM_ENQUANTO'
	private static void cmd_enquanto() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		if ( token == T_ENQUANTO ) {
			buscaProximoToken();
			if(token == T_ABRE_PAR) {
				buscaProximoToken();
				condicao();
				regraSemantica( 25 );
			}
			if(token == T_FECHA_PAR) {
				buscaProximoToken();
			}
			if(token == T_ABRE_CHAVES) {
				buscaProximoToken();
				cmds();
				regraSemantica( 17 );
			}
			if ( token == T_FIM_ENQUANTO || token == T_FECHA_CHAVES ) {
				buscaProximoToken();
				acumulaRegraSintaticaReconhecida( "<CMD_ENQUANTO> ::= 'WHILE' <CONDICAO> <CMDS> 'FIM_ENQUANTO'" );
			} else {
				registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'fim enquanto' esperado mas encontrei: " + lexema );
			}
		} else {
			registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'enquanto' esperado mas encontrei: " + lexema );
		}
	}

	// <CMD_PARA> ::= 'PARA' <VAR> '<-' <E> 'ATE' <E> <CMDS> 'FIM_PARA'
	private static void cmd_para() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		if ( token == T_PARA ) {
			buscaProximoToken();
			var();
			if ( token == T_SETA ) {
				buscaProximoToken();
				e();
				if ( token == T_ATE ) {
					buscaProximoToken();
					e();
					regraSemantica(18);
					cmds();
					regraSemantica(17);
					if ( token == T_FIM_PARA ) {
						buscaProximoToken();
						acumulaRegraSintaticaReconhecida( "<CMD_PARA> ::= 'PARA' <VAR> '<-' <E> 'ATE' <E> <CMDS> 'FIM_PARA'" );
					} else {
						registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'fim_para' esperado mas encontrei: " + lexema );
					}
				} else {
					registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'Ate' esperado mas encontrei: " + lexema );
				}
			} else {
				registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'<-' esperado mas encontrei: " + lexema );
			}
		} else {
			registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'Para' esperado mas encontrei: " + lexema );
		}
	}

	// <CMD_ATRIBUICAO> ::= <VAR> '<-' <E>
	private static void cmd_atribuicao() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		var();
		if ( token == T_SETA ) {
			buscaProximoToken();
			e();
			regraSemantica( 3 );
			acumulaRegraSintaticaReconhecida("<CMD_ATRIBUICAO> ::= <VAR> '<-' <E>");
		}else if(token == T_MAIS) {
			buscaProximoToken();
			e();
		} else {
			registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'<-' esperado mas encontrei: " + lexema );
		}
	}

	// <CMD_LER> ::= 'LER' '(' <VAR> ')'
	private static void cmd_ler() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
		if ( token == T_LER ) {
			buscaProximoToken();
			if ( token == T_ABRE_PAR ) {
				buscaProximoToken();
				var();
				regraSemantica( 14 );
				if ( token == T_FECHA_PAR ) {
					buscaProximoToken();
					acumulaRegraSintaticaReconhecida( "<CMD_LER> ::= 'LER' '(' <VAR> ')'" );
				} else {
					registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n')' esperado mas encontrei: " + lexema );
				}
			} else {
				registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'(' esperado mas encontrei: " + lexema );
			}
		} else {
			registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'Ler' esperado mas encontrei: " + lexema );
		}
	}

  // <CMD_ESCREVER> ::= 'ESCREVER' '(' <E> ')'
  private static void cmd_escrever() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  if ( token == T_ESCREVER ) {
		  buscaProximoToken();
		  if ( token == T_ABRE_PAR ) {
			  buscaProximoToken();
			  e();
			  regraSemantica( 15 );
			  if ( token == T_FECHA_PAR ) {
				  buscaProximoToken();
				  acumulaRegraSintaticaReconhecida( "<CMD_ESCREVER> ::= 'ESCREVER' '(' <E> ')'" );
			  } else {
				  registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n')' esperado mas encontrei: " + lexema );
			  }
		  } else {
			  registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'(' esperado mas encontrei: " + lexema ); 
		  }
	  } else {
		  registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n'Escrever' esperado mas encontrei: " + lexema );
	  }
  }

  // <CONDICAO> ::= <E> '>' <E> 
  // <CONDICAO> ::= <E> '>=' <E> 
  // <CONDICAO> ::= <E> '<>' <E> 
  // <CONDICAO> ::= <E> '<=' <E> 
  // <CONDICAO> ::= <E> '<' <E> 
  // <CONDICAO> ::= <E> '==' <E>
  private static void condicao() throws ErroLexicoException, IOException, ErroSintaticoException, ErroSemanticoException {
	  
	  e();
	  switch ( token ) {
	  case T_MAIOR: buscaProximoToken(); e(); regraSemantica(19); break; 
	  case T_MENOR: buscaProximoToken(); e(); regraSemantica(20); break; 
	  case T_MAIOR_IGUAL: buscaProximoToken(); e(); regraSemantica(21);break; 
	  case T_MENOR_IGUAL: buscaProximoToken(); e(); regraSemantica(22); break; 
	  case T_IGUAL: buscaProximoToken(); e(); regraSemantica(23); break; 
	  case T_DIFERENTE: buscaProximoToken(); e(); regraSemantica(24); break; 
	  default: registraErroSintatico( "Erro Sintatico. Linha: " + linhaAtual + "\nColuna: " + colunaAtual + "\nErro: <" + linhaFonte + ">\nEsperava um operador logico. Encontrei: " + lexema );
	  }
	  acumulaRegraSintaticaReconhecida( "<CONDICAO> ::= <E> ('>'|'>='|'<>'|'<='|'<'|'==') <E> " );
  }
  
  // <E> ::= <E> + <T>
  // <E> ::= <E> - <T>
  // <E> ::= <T>
  private static void e() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  t();
	  while ( (token == T_MAIS) || (token == T_MENOS) ) {
		  switch( token ) {
	      case T_MAIS: { buscaProximoToken();
		  				 t();
		  				 regraSemantica( 5 );
	      	   }
	           break;
	      case T_MENOS: { buscaProximoToken();
	      				  t();
	      				  regraSemantica( 6 );
	      	   }
	           break;
		  }
	  }
	  acumulaRegraSintaticaReconhecida( "<E> ::= <E> + <T>|<E> - <T>|<T> " );
  }
  
  // <T> ::= <T> * <F>
  // <T> ::= <T> / <F>
  // <T> ::= <T> % <F>
  // <T> ::= <F>
  private static void t() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  f();
	  while ( (token == T_VEZES) || (token == T_DIVIDIDO) || (token == T_RESTO) ) {
		  switch ( token ) {
		  case T_VEZES: { buscaProximoToken();
		  				  f();
		  				  regraSemantica( 7 );
		  				}
		                break;
		  case T_DIVIDIDO: { buscaProximoToken();
			  					f();
			  					regraSemantica( 8 );
						   }
		                break;
		  case T_RESTO: { buscaProximoToken();
			 		  	  f();
			 		  	  regraSemantica( 9 );
		  				}
		                break;
		  }
	  }
	  acumulaRegraSintaticaReconhecida( "<T> ::= <T> * <F>|<T> / <F>|<T> % <F>|<F>" );
  }
  
  // <F> ::= -<F>
  // <F> ::= <X> ** <F>
  // <F> ::= <X>     
  private static void f() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  if ( token == T_MENOS ) {
		  buscaProximoToken();
		  f();
	  } else {
		  x();
		  while ( token == T_ELEVADO ) {
			  buscaProximoToken();
	          x();
	          regraSemantica( 10 );
		  }
	  }
	  acumulaRegraSintaticaReconhecida( "<F> ::= -<F>|<X> ** <F>|<X> " );
	  
  }
  
  // <X> ::= '(' <E> ')'
  // <X> ::= [0-9]+('.'[0-9]+)
  // <X> ::= <VAR>
  private static void x() throws IOException, ErroLexicoException, ErroSintaticoException, ErroSemanticoException {
	  switch ( token ) {
	  case T_IDENTIFICADOR: buscaProximoToken(); acumulaRegraSintaticaReconhecida( "<X> ::= <VAR>" ); regraSemantica( 11 ); break;
	  case T_NUMERO: buscaProximoToken(); acumulaRegraSintaticaReconhecida( "<X> ::= [0-9]+('.'[0-9]+)" ); regraSemantica( 12 ); break;
	  case T_ABRE_PAR: {
	       buscaProximoToken(); 
	       e();
	       if ( token == T_FECHA_PAR ) {
	    	   buscaProximoToken();
	    	   acumulaRegraSintaticaReconhecida( "<X> ::= '(' <E> ')'" );
	       } else {
			   registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\n')' esperado mas encontrei: " + lexema );
	       }
	       regraSemantica( 13 );
	      } break;
	   default: registraErroSintatico( "Erro Sintatico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\nFator invalido: encontrei: " + lexema );   
	  }
  }
  
  static void fechaFonte() throws IOException
  {
      rdFonte.close();
  }

  static void movelookAhead() throws IOException
  {
	  
    if ( ( ponteiro + 1 ) > linhaFonte.length() ) {

    	linhaAtual++;
        ponteiro = 0;
        
        
        if ( ( linhaFonte = rdFonte.readLine() ) == null ) {
            lookAhead = FIM_ARQUIVO;
        } else {

        	StringBuffer sbLinhaFonte = new StringBuffer( linhaFonte );
        	sbLinhaFonte.append( '\13' ).append( '\10' );
        	linhaFonte = sbLinhaFonte.toString();
        	
            lookAhead = linhaFonte.charAt( ponteiro );
        }
    } else {
        lookAhead = linhaFonte.charAt( ponteiro );
    }

    // Se comentar esse if, eu terei uma linguagem 
    // que diferencia minusculas de maiusculas
    if ( ( lookAhead >= 'a' ) &&
         ( lookAhead <= 'z' ) ) {
        lookAhead = (char) ( lookAhead - 'a' + 'A' );
    }

    ponteiro++;
    colunaAtual = ponteiro + 1;
  }

  static void buscaProximoToken() throws IOException, ErroLexicoException
  {
	//int i, j;
	  
	if ( lexema != null ) {
	    ultimoLexema = new String( lexema );
	} 
        
    StringBuffer sbLexema = new StringBuffer( "" );

    // Salto espaçoes enters e tabs até o inicio do proximo token
  	while ( ( lookAhead == 9 ) ||
	        ( lookAhead == '\n' ) ||
	        ( lookAhead == 8 ) ||
	        ( lookAhead == 11 ) ||
	        ( lookAhead == 12 ) ||
	        ( lookAhead == '\r' ) ||
	        ( lookAhead == 32 ) )
    {
        movelookAhead();
    }

    /*--------------------------------------------------------------*
     * Caso o primeiro caracter seja alfabetico, procuro capturar a *
     * sequencia de caracteres que se segue a ele e classifica-la   *
     *--------------------------------------------------------------*/
    if ( ( lookAhead >= 'A' ) && ( lookAhead <= 'Z' ) ) {   
        sbLexema.append( lookAhead );
        movelookAhead();

        while ( ( ( lookAhead >= 'A' ) && ( lookAhead <= 'Z' ) ) ||
        		( ( lookAhead >= '0' ) && ( lookAhead <= '9' ) ) || ( lookAhead == '_' ) )
        {
            sbLexema.append( lookAhead );
            movelookAhead();
        }

        lexema = sbLexema.toString();

		/* Classifico o meu token como palavra reservada ou id */
		if ( lexema.equals( "PROGRAMA" ) )
			token = T_ALGORITMO;
		else if ( lexema.equals( "FIM") )
			token = T_FIM_ALGORITMO;
		else if ( lexema.equals( "VARIAVEIS" ) )
			token = T_DEF_VARIAVEIS;
		else if ( lexema.equals( "VIRGULA" ) )
			token = T_VIRGULA;
		else if ( lexema.equals( "FIM_LINHA" ) )
			token = T_FIM_LINHA;
		else if ( lexema.equals( "IF" ) )
			token = T_SE;
		else if ( lexema.equals( "ELSE" ) )
			token = T_SENAO;
		else if ( lexema.equals( "FIM_SE" ) )
			token = T_FIM_SE;
		else if ( lexema.equals( "WHILE" ) )
			token = T_ENQUANTO;
		else if ( lexema.equals( "FIM_ENQUANTO" ) )
			token = T_FIM_ENQUANTO;
		else if ( lexema.equals( "PARA" ) )
			token = T_PARA;
		else if ( lexema.equals( "SETA" ) )
			token = T_SETA;
		else if ( lexema.equals( "ATE" ) )
			token = T_ATE;
		else if ( lexema.equals( "FIM_PARA" ) )
			token = T_FIM_PARA;
		else if ( lexema.equals( "LER" ) )
			token = T_LER;
		else if ( lexema.equals( "ABRE_PAR" ) )
			token = T_ABRE_PAR;
		else if ( lexema.equals( "FECHA_PAR" ) )
			token = T_FECHA_PAR;
		else if ( lexema.equals( "ESCREVER" ) )
			token = T_ESCREVER;
		else if ( lexema.equals( "MAIOR" ) )
			token = T_MAIOR;
		else if ( lexema.equals( "MENOR" ) )
			token = T_MENOR;
		else if ( lexema.equals( "MAIOR_IGUAL" ) )
			token = T_MAIOR_IGUAL;
		else if ( lexema.equals( "MENOR_IGUAL" ) )
			token = T_MENOR_IGUAL;
		else if ( lexema.equals( "IGUAL" ) )
			token = T_IGUAL;
		else if ( lexema.equals( "DIFERENTE" ) )
			token = T_DIFERENTE;
		else if ( lexema.equals( "MAIS" ) )
			token = T_MAIS;
		else if ( lexema.equals( "MENOS" ) )
			token = T_MENOS;
		else if ( lexema.equals( "VEZES" ) )
			token = T_VEZES;
		else if ( lexema.equals( "DIVIDIDO" ) )
			token = T_DIVIDIDO;
		else if ( lexema.equals( "RESTO" ) )
			token = T_RESTO;
		else if ( lexema.equals( "ELEVADO" ) )
			token = T_ELEVADO;
		else if ( lexema.equals( "NUMERO" ) )
			token = T_NUMERO;
		else if ( lexema.equals( "IDENTIFICADOR" ) )
			token = T_IDENTIFICADOR;
		else if ( lexema.equals( "FIM_FONTE" ) )
			token = T_FIM_FONTE;
		else if ( lexema.equals( "ERRO_LEX" ) )
			token = T_ERRO_LEX;
		else if ( lexema.equals( "NULO" ) )
			token = T_NULO;
		else if(lexema.equals("PONTO_VIRGULA"))
			token = T_PONTO_VIRGULA;
		else if(lexema.equals("ABRE_CHAVES"))
			token = T_ABRE_CHAVES;
		else if(lexema.equals("FECHA_CHAVES"))
			token = T_FECHA_CHAVES;
		else {
			token = T_IDENTIFICADOR;
		}
    } else if ( ( lookAhead >= '0' ) && ( lookAhead <= '9' ) ) {
        sbLexema.append( lookAhead );
        movelookAhead();
        while ( ( lookAhead >= '0' ) && ( lookAhead <= '9' ) )
        {
            sbLexema.append( lookAhead );
            movelookAhead();
        }
		token = T_NUMERO;
	} else if ( lookAhead == '(' ){
		sbLexema.append( lookAhead );
		token = T_ABRE_PAR;
		movelookAhead();
	} else if ( lookAhead == ')' ){
		sbLexema.append( lookAhead );
		token = T_FECHA_PAR;
		movelookAhead();
	} else if ( lookAhead == ';' ){
		sbLexema.append( lookAhead );
		token = T_PONTO_VIRGULA;
		movelookAhead();
	} else if(lookAhead == '{') {
		sbLexema.append(lookAhead);
		token = T_ABRE_CHAVES;
		movelookAhead();
	}else if(lookAhead == '}') {
		sbLexema.append(lookAhead);
		token = T_FECHA_CHAVES;
		movelookAhead();
	}else if ( lookAhead == ',' ){
		sbLexema.append( lookAhead );
		token = T_VIRGULA;
		movelookAhead();
	} else if ( lookAhead == '+' ){
		sbLexema.append( lookAhead );
		token = T_MAIS;
		movelookAhead();
	} else if ( lookAhead == '-' ){
		sbLexema.append( lookAhead );
		token = T_MENOS;
		movelookAhead();
	} else if ( lookAhead == '*' ){
		sbLexema.append( lookAhead );
		movelookAhead();
		if ( lookAhead == '*' ) {
			sbLexema.append( lookAhead );
			movelookAhead();
			token = T_ELEVADO;
		} else {
			token = T_VEZES;
		}
    } else if ( lookAhead == '=' ){
        sbLexema.append( lookAhead );
        movelookAhead();
        if ( lookAhead == '=' ) {
            sbLexema.append( lookAhead );
            movelookAhead();
            token = T_IGUAL;    	
        }  else {
        	token = T_ERRO_LEX;
        }
    } else if ( lookAhead == '/' ){
        sbLexema.append( lookAhead );
        token = T_DIVIDIDO;    	
        movelookAhead();
    } else if ( lookAhead == '%' ){
        sbLexema.append( lookAhead );
        token = T_RESTO;    	
        movelookAhead();
    } else if ( lookAhead == '<' ){
        sbLexema.append( lookAhead );
        movelookAhead();
        if ( lookAhead == '>' ) {
            sbLexema.append( lookAhead );
            movelookAhead();
            token = T_DIFERENTE;
        } else if ( lookAhead == '-' ) {  
            sbLexema.append( lookAhead );
            movelookAhead();
            token = T_SETA;
        } else if ( lookAhead == '=' ) {  
            sbLexema.append( lookAhead );
            movelookAhead();
            token = T_MENOR_IGUAL;
        } else {
            token = T_MENOR;    	
        }
    } else if ( lookAhead == '>' ){
        sbLexema.append( lookAhead );
        movelookAhead();
        if ( lookAhead == '=' ) {
            sbLexema.append( lookAhead );
            movelookAhead();
            token = T_MAIOR_IGUAL;
        } else {
            token = T_MAIOR;    	
        }        
    } else if ( lookAhead == FIM_ARQUIVO ){
         token = T_FIM_FONTE;    	
    } else {
    	token = T_ERRO_LEX;
    	sbLexema.append( lookAhead );
    }
        
    lexema = sbLexema.toString();  
    
    mostraToken();
    
    if ( token == T_ERRO_LEX ) {
    	mensagemDeErro = "Erro Léxico na linha: " + linhaAtual + "\nReconhecido ao atingir a coluna: " + colunaAtual + "\nLinha do Erro: <" + linhaFonte + ">\nToken desconhecido: " + lexema;
    	throw new ErroLexicoException( mensagemDeErro );
    } 
  }

	static void mostraToken()
	{
		StringBuffer tokenLexema = new StringBuffer( "" );
		switch ( token ) {
			case T_ALGORITMO         : tokenLexema.append( "T_ALGORITMO" ); break;
			case T_FIM_ALGORITMO     : tokenLexema.append( "T_FIM_ALGORITMO" ); break;
			case T_DEF_VARIAVEIS     : tokenLexema.append( "T_DEF_VARIAVEIS" ); break;
			case T_VIRGULA           : tokenLexema.append( "T_VIRGULA" ); break;
			case T_PONTO_VIRGULA     : tokenLexema.append( "T_PONTO_VIRGULA" ); break;
			case T_ABRE_CHAVES       : tokenLexema.append("T_ABRE_CHAVES"); break;
			case T_FECHA_CHAVES      : tokenLexema.append("T_FECHA_CHAVES"); break;
			case T_FIM_LINHA         : tokenLexema.append( "T_FIM_LINHA" ); break;
			case T_SE                : tokenLexema.append( "T_SE" ); break;
			case T_SENAO             : tokenLexema.append( "T_SENAO" ); break;
			case T_FIM_SE            : tokenLexema.append( "T_FIM_SE" ); break;
			case T_ENQUANTO          : tokenLexema.append( "T_ENQUANTO" ); break;
			case T_FIM_ENQUANTO      : tokenLexema.append( "T_FIM_ENQUANTO" ); break;
			case T_PARA              : tokenLexema.append( "T_PARA" ); break;
			case T_SETA              : tokenLexema.append( "T_SETA" ); break;
			case T_ATE               : tokenLexema.append( "T_ATE" ); break;
			case T_FIM_PARA          : tokenLexema.append( "T_FIM_PARA" ); break;
			case T_LER               : tokenLexema.append( "T_LER" ); break;
			case T_ABRE_PAR          : tokenLexema.append( "T_ABRE_PAR" ); break;
			case T_FECHA_PAR         : tokenLexema.append( "T_FECHA_PAR" ); break;
			case T_ESCREVER          : tokenLexema.append( "T_ESCREVER" ); break;
			case T_MAIOR             : tokenLexema.append( "T_MAIOR" ); break;
			case T_MENOR             : tokenLexema.append( "T_MENOR" ); break;
			case T_MAIOR_IGUAL       : tokenLexema.append( "T_MAIOR_IGUAL" ); break;
			case T_MENOR_IGUAL       : tokenLexema.append( "T_MENOR_IGUAL" ); break;
			case T_IGUAL             : tokenLexema.append( "T_IGUAL" ); break;
			case T_DIFERENTE         : tokenLexema.append( "T_DIFERENTE" ); break;
			case T_MAIS              : tokenLexema.append( "T_MAIS" ); break;
			case T_MENOS             : tokenLexema.append( "T_MENOS" ); break;
			case T_VEZES             : tokenLexema.append( "T_VEZES" ); break;
			case T_DIVIDIDO          : tokenLexema.append( "T_DIVIDIDO" ); break;
			case T_RESTO             : tokenLexema.append( "T_RESTO" ); break;
			case T_ELEVADO           : tokenLexema.append( "T_ELEVADO" ); break;
			case T_NUMERO            : tokenLexema.append( "T_NUMERO" ); break;
			case T_IDENTIFICADOR     : tokenLexema.append( "T_IDENTIFICADOR" ); break;
			case T_FIM_FONTE         : tokenLexema.append( "T_FIM_FONTE" ); break;
			case T_ERRO_LEX          : tokenLexema.append( "T_ERRO_LEX" ); break;
			case T_NULO              : tokenLexema.append( "T_NULO" ); break;
			default                : tokenLexema.append( "N/A" ); break;
		}
		System.out.println( tokenLexema.toString() + " ( " + lexema + " )" );
		acumulaToken( tokenLexema.toString() + " ( " + lexema + " )" );
		tokenLexema.append( lexema );
	}
  
  private static void abreArquivo() {

		JFileChooser fileChooser = new JFileChooser();
		
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

		FiltroSab filtro = new FiltroSab();
	    
		fileChooser.addChoosableFileFilter( filtro );
		int result = fileChooser.showOpenDialog( null );
		
		if( result == JFileChooser.CANCEL_OPTION ) {
			return;
		}
		
		arqFonte = fileChooser.getSelectedFile();
		abreFonte( arqFonte ); 	

	}


	private static boolean abreFonte( File fileName ) {

		if( arqFonte == null || fileName.getName().trim().equals( "" ) ) {
			JOptionPane.showMessageDialog( null, "Nome de Arquivo Invalido", "Nome de Arquivo Invalido", JOptionPane.ERROR_MESSAGE );
			return false;
		} else {
			linhaAtual = 1;
	        try {
				FileReader fr = new FileReader( arqFonte );
				rdFonte = new BufferedReader( fr );
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} 
			return true;
		}
	}

	private static void abreDestino() {

		JFileChooser fileChooser = new JFileChooser();
			
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

		FiltroSab filtro = new FiltroSab();
		    
		fileChooser.addChoosableFileFilter( filtro );
		int result = fileChooser.showSaveDialog( null );
			
		if( result == JFileChooser.CANCEL_OPTION ) {
			return;
		}
			
		arqDestino = fileChooser.getSelectedFile();
	}	

	private static boolean gravaSaida( File fileName ) {

		if( arqDestino == null || fileName.getName().trim().equals( "" ) ) {
			JOptionPane.showMessageDialog( null, "Nome de Arquivo Invalido", "Nome de Arquivo Invalido", JOptionPane.ERROR_MESSAGE );
			return false;
		} else {
			FileWriter fw;
			try {
				fw = new FileWriter( arqDestino );
				BufferedWriter bfw = new BufferedWriter( fw ); 
                bfw.write( codigoPython.toString() );
                bfw.close();
				JOptionPane.showMessageDialog( null, "Arquivo Salvo: " + arqDestino, "Salvando Arquivo", JOptionPane.INFORMATION_MESSAGE );
			} catch (IOException e) {
				JOptionPane.showMessageDialog( null, e.getMessage(), "Erro de Entrada/Saida", JOptionPane.ERROR_MESSAGE );
			} 
			return true;
		}
	}
	
	
	public static void exibeTokens() {
		
		JTextArea texto = new JTextArea();
		texto.append( tokensIdentificados.toString() );
		JOptionPane.showMessageDialog(null, texto, "Tokens Identificados (token/lexema)", JOptionPane.INFORMATION_MESSAGE );
	}
	
	
	public static void acumulaRegraSintaticaReconhecida( String regra ) {

		regrasReconhecidas.append( regra );
		regrasReconhecidas.append( "\n" );
		
	}
	
	public static void acumulaToken( String tokenIdentificado ) {

		tokensIdentificados.append( tokenIdentificado );
		tokensIdentificados.append( "\n" );
		
	}
	
    public static void exibeSaida() {

        JTextArea texto = new JTextArea();
        texto.append( tokensIdentificados.toString() );
        JOptionPane.showMessageDialog(null, texto, "Analise Lexica", JOptionPane.INFORMATION_MESSAGE );

        texto.setText( regrasReconhecidas.toString() );
        texto.append( "\n\nStatus da Compilacao:\n\n" );
        texto.append( mensagemDeErro );

        JOptionPane.showMessageDialog(null, texto, "Resumo da Compilacao", JOptionPane.INFORMATION_MESSAGE );
    }
    
    static void registraErroSintatico( String msg ) throws ErroSintaticoException {
        if ( estadoCompilacao == E_SEM_ERROS ) {
            estadoCompilacao = E_ERRO_SINTATICO;
            mensagemDeErro = msg;
        }
        throw new ErroSintaticoException( msg ); 
    }
    
    static void registraErroSemantico( String msg ) throws ErroSemanticoException {

        if ( estadoCompilacao == E_SEM_ERROS ) {
            estadoCompilacao = E_ERRO_SEMANTICO;
            mensagemDeErro = msg;
        }
        throw new ErroSemanticoException( msg ); 
    }

    static void regraSemantica( int numeroRegra ) throws ErroSemanticoException {
        System.out.println( "Regra Semantica " + numeroRegra );
        switch ( numeroRegra ) {
        case  0: 	codigoPython.append( "\nimport os\nimport sys\nimport glob\nimport string\n\n\n" );
        			codigoPython.append( "def main( ):\n" );
        			nivelIdentacao++;
        			codigoPython.append( tabulacao( nivelIdentacao ) );
        			codigoPython.append( "# Feevale compiler\n\n\n");
        			break;
        case  1: 	codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "pass\n\n" );
					codigoPython.append( "if __name__ == '__main__':\n" );
					codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "main( )\n" );
					break;
        case  2:	insereNaTabelaSimbolos( ultimoLexema );
					break;
        case  3:	nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();	
					System.out.println( "Codigo 1 " + nodo_1.getCodigo() );
					System.out.println( "Codigo 2 " + nodo_2.getCodigo() );
					codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( nodo_1.getCodigoMinusculo() + " = " + nodo_2.getCodigoMinusculo() + "\n" );
					break;					
        case  4:	if ( VeSeExisteNaTabelaSimbolos( ultimoLexema ) ) {
            			pilhaSemantica.push( ultimoLexema, 4 );
        			}
        			break;
        case  5:    nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "+" + nodo_2.getCodigoMinusculo(), 5 );
					break;
        case  6:    nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "-" + nodo_2.getCodigoMinusculo(), 6 );
					break;
        case  7:    nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "*" + nodo_2.getCodigoMinusculo(), 7 );
					break;
        case  8:    nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "/" + nodo_2.getCodigoMinusculo(), 8 );
					break;
        case  9:    nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "%" + nodo_2.getCodigoMinusculo(), 9 );
					break;
        case 10:    nodo_2 = pilhaSemantica.pop();
        			nodo_1 = pilhaSemantica.pop();
        			pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "**" + nodo_2.getCodigoMinusculo(), 10 );
        			break;
        case 11:	if ( VeSeExisteNaTabelaSimbolos( ultimoLexema ) ) {
						pilhaSemantica.push( ultimoLexema, 11 );
					}
					break;
        case 12:	pilhaSemantica.push( ultimoLexema, 12 );
					break;
        case 13:	nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( "(" + nodo_1.getCodigoMinusculo() + ")" , 13 );            
					break;
        case 14:	nodo_1 = pilhaSemantica.pop();
   		            codigoPython.append( tabulacao( nivelIdentacao ) );
   		            codigoPython.append( nodo_1.getCodigoMinusculo() + " = int( input( 'digite a variavel ' ) ) \n" );
					break;					
        case 15:	nodo_1 = pilhaSemantica.pop();
           			codigoPython.append( tabulacao( nivelIdentacao ) );
           			codigoPython.append( "print ( " + nodo_1.getCodigoMinusculo() + " )\n" );
           			break;		
        case 16:	nodo_1 = pilhaSemantica.pop();
        			codigoPython.append( tabulacao( nivelIdentacao ) );
        			codigoPython.append( "for i" + nivelIdentacao + " in ( range " + nodo_1.getCodigoMinusculo() + " ):\n" );
        			nivelIdentacao++;
        			break;
        case 17:	nivelIdentacao--;
        			break;
        case 18:	nodo_3 = pilhaSemantica.pop();
        			nodo_2 = pilhaSemantica.pop();
        			nodo_1 = pilhaSemantica.pop();
        			codigoPython.append( tabulacao( nivelIdentacao ) );
        			codigoPython.append( "for " + nodo_1.getCodigoMinusculo() + " in range( " + nodo_2.getCodigoMinusculo() + ", " + nodo_3.getCodigoMinusculo() + "+1 ):\n" );
        			nivelIdentacao++;
        			break;
        case 19:	nodo_2 = pilhaSemantica.pop();
        			nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + ">" + nodo_2.getCodigoMinusculo(), 19 );
        			break;
        case 20:	System.out.println( " Entrou na 20" );
                    JOptionPane.showMessageDialog(null, " Entrou na 20");
        	        nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "<" + nodo_2.getCodigoMinusculo(), 20 );
					break;
        case 21:	nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + ">=" + nodo_2.getCodigoMinusculo(), 21 );
					break;
        case 22:	nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "<=" + nodo_2.getCodigoMinusculo(), 22 );
					break;
        case 23:	nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "==" + nodo_2.getCodigoMinusculo(), 23 );
					break;
        case 24:	nodo_2 = pilhaSemantica.pop();
					nodo_1 = pilhaSemantica.pop();
					pilhaSemantica.push( nodo_1.getCodigoMinusculo() + "!=" + nodo_2.getCodigoMinusculo(), 24 );
					break;
        case 25:	nodo_1 = pilhaSemantica.pop();
					codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "while ( " + nodo_1.getCodigoMinusculo() + " ):\n" );
					nivelIdentacao++;
					break;
        case 26:    codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "while True:\n" );
					nivelIdentacao++;
					break;
        case 27:	nodo_1 = pilhaSemantica.pop();
					codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "if " + nodo_1.getCodigoMinusculo() + ":\n" );
					nivelIdentacao++;
					codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "break\n" );
					nivelIdentacao-=2;
					break;
        case 28:	nodo_1 = pilhaSemantica.pop();
					codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "if " + nodo_1.getCodigoMinusculo() + ":\n" );
					nivelIdentacao++;
					break;
        case 29:	nivelIdentacao--;
        			codigoPython.append( tabulacao( nivelIdentacao ) );
					codigoPython.append( "else:\n " );
					nivelIdentacao++;
					break;
        case 30:	nivelIdentacao--;
        			break;

        }
    }
    
    private static boolean VeSeExisteNaTabelaSimbolos(String ultimoLexema ) throws ErroSemanticoException {
    	if ( !tabelaSimbolos.containsKey( ultimoLexema ) ) {
	    	throw new ErroSemanticoException( "Variavel " + ultimoLexema + " nao esta declarada! linha: " + linhaAtual );
    	} else {
    		return true;
    	}
    }

	private static void insereNaTabelaSimbolos(String ultimoLexema) throws ErroSemanticoException {
		if ( tabelaSimbolos.containsKey( ultimoLexema ) ) {
	    	throw new ErroSemanticoException( "Variavel " + ultimoLexema + " ja declarada! linha: " + linhaAtual );
		} else {
			tabelaSimbolos.put( ultimoLexema, 0 );
		}
	}
    
	static String tabulacao( int qtd ) {
        StringBuffer sb = new StringBuffer();
        for ( int i=0; i<qtd; i++ ) {
            sb.append( "    " );
        }
        return sb.toString();
    }
		
}

class FiltroSab extends FileFilter {

	public boolean accept(File arg0) {
	   	 if(arg0 != null) {
	         if(arg0.isDirectory()) {
	       	  return true;
	         }
	         if( getExtensao(arg0) != null) {
	        	 if ( getExtensao(arg0).equalsIgnoreCase( "grm" ) ) {
		        	 return true;
	        	 }
	         };
	   	 }
	     return false;
	}

	public String getDescription() {
		return "*.grm";
	}

	public String getExtensao(File arq) {
	if(arq != null) {
		String filename = arq.getName();
	    int i = filename.lastIndexOf('.');
	    if(i>0 && i<filename.length()-1) {
	    	return filename.substring(i+1).toLowerCase();
	    };
	}
		return null;
	}
}
