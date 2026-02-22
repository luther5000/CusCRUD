Funcionalidade: Editar um produto
	Como usuário do sistema
	Quero alterar as informações de um produto
	Para atualizar as informações do sistema

	Contexto:
		Dado que o usuário visualiza um produto adicionado
		E seleciona a opção de editar produto

	Cenário: Edição do produto
		Quando o usuário insere as novas informações do produto
		E confirma a edição das informações
		Então o usuário visualiza a confirmação da edição
		E retorna para a tela inicial do inventário

	Cenário: Cancelar a ação
		Quando o usuário insere as novas informações do produto
		E seleciona o botão "cancelar"
		Então o usuário retorna para a tela inicial do seu inventário
		E nenhuma informação é salva no sistema

	Cenário: Campos obrigatórios deixados em branco
		Quando o usuário insere as novas informações do produto
		Mas deixa campos obrigatórios em branco
		E confirma a edição das informações
		Então o sistema informa o usuário que é necessário preencher todos os campos obrigatórios para fazer a edição
		E nenhuma informação é salva no sistema

	Cenário: Quantidade negativa
		Quando o usuário insere as novas informações do produto
		Mas insere quantidade negativa
		E confirma a edição das informações
		Então o sistema informa o usuário que é necessário informar uma quantidade positiva para fazer a adição
		E nenhuma informação é salva no sistema
