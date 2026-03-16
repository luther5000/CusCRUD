# language: pt

Funcionalidade: Visualização de inventário e produtos
	Como usuário do sistema
	Quero visualizar os produtos de meu inventário
	Para acompanhar os produtos disponíveis no estoque

	Contexto:
		Dado que existam produtos adicionados ao sistema

	Cenário: Visualização do inventário geral
		Quando o usuário entra na página inicial do inventário
		Então ele visualiza as informações dos produtos agrupados por tipo

	Cenário: Visualização dos produtos de um tipo
		Dado que o usuário está na página inicial do inventário
		Quando ele seleciona um tipo de alimento
		Então ele visualiza os diversos produtos existentes do tipo selecionado

	Cenário: Visualização de detalhes dos produtos
		Dado que o usuário está visualizando os produtos de um tipo
		Quando ele seleciona um daqueles produtos
		Então ele visualiza as informações detalhadas do produto selecionado

	Cenário: Erro interno do sistema na visualização do inventário geral
		Dado que ocorre um erro interno no sistema
		Quando o usuário entra na página inicial do inventário
		Então o usuário visualiza uma mensagem informando que não foi possível carregá-lo

	Cenário: Erro interno do sistema na visualização dos produtos de um tipo
		Dado que ocorre um erro interno no sistema
		E que o usuário está na página inicial do inventário
		Quando ele seleciona um tipo de produto
		Então o usuário visualiza uma mensagem informando que não foi possível carregá-los
	
	Cenário: Erro interno do sistema na visualização de detalhes dos produtos
		Dado que ocorre um erro interno no sistema
		E que o usuário está visualizando os produtos de um tipo
		Quando ele seleciona um daqueles produtos
		Então o usuário visualiza uma mensagem informando que não foi possível carregá-lo
