Funcionalidade: Alterar quantidade de ítens um produto
	Como usuário do sistema
	Quero alterar a quantidade de ítens de um produto
	Para refletir o inventário com o meu estoque real

	Contexto:
		Dado que o usuário visualiza um produto

	Cenário: Aumentar em um a quantidade de ítens do produto
		Quando o usuário clica na opção de aumentar em um a quantidade de ítens
		Então o usuário visualiza que existe um ítem a mais do produto
	
	Cenário: Diminuir em um a quantidade de ítens do produto
		Quando o usuário clica na opção de diminuir em um a quantidade de ítens
		Então o usuário visualiza que existe um ítem a menos do produto
	
	Cenário: Diminuir em um a quantidade de ítens de um produto com zero ítens
		Dado que o produto possui zero ítens
		Quando o usuário clica na opção de diminuir em um a quantidade ítens
		Então o usuário visualiza que a quantidade de ítens não foi alterada
