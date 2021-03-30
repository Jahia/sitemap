const gqlMutate = (client, mutationName, variables) => {
    return client.mutate({
        mutation: mutationName,
        variables: variables
    }).then(data => {
        return data;
    });
};

const gqlQuery = (client, queryName, variables) => {
    return client.query({
        query: queryName,
        variables: variables
    }).then(data => {
        return data;
    });
};

export {gqlMutate, gqlQuery};
