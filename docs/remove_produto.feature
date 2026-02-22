Funcionalidade: Remoção de produtos
	Como usuário do sistema
	Quero remover um produto do meu inventário
	Para atualizar as informações do inventário

	Contexto:
		Dado que o usuário está no seu inventário
		E possui produtos adicionados

	Cenário: Remover produto
		Quando o usuário seleciona um produto do inventário
		E seleciona a opção "remover"
		Então o usuário visualiza a confirmação de remoção 
		E retorna para a página inicial do meu inventário
