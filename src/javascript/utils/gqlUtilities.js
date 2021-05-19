const gqlMutate = (client, mutationName, variables) => {
    return client.mutate({
        mutation: mutationName,
        variables: variables
    }).then(data => {
        return data;
    }).catch(error => console.error(error));
};

export {gqlMutate};
