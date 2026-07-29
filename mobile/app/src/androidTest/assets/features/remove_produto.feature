# language: pt

Funcionalidade: Remoção de produtos
	Como usuário do sistema
	Quero remover um produto do meu inventário
	Para atualizar as informações do inventário

	Contexto:
		Dado que o usuário está visualizando os produtos de um tipo de alimento

	Cenário: Remover produto
		Quando o usuário seleciona um produto
		E clica na opção de remover este produto do inventário
		E confirma a remoção
		Então o usuário visualiza a confirmação de remoção 
		E retorna para onde estava antes de realizar a remoção
	
	Cenário: Erro interno do sistema
		Dado que ocorre um erro interno no sistema
		Quando clica na opção de remover este produto do inventário
		E clica na opção de remover este produto do inventário
		E confirma a remoção
		Então o usuário visualiza o produto no inventário
		E uma mensagem informando que não foi possível realizar a remoção
