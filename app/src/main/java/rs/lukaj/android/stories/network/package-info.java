/**
 * Contains helpers for communicating with the server. Network holds data only, and Books and Users
 * send and receive information from appropriate server endpoints. Methods in Users and Books classes
 * are public and static. TokenManager is used by the network library to resolve auth tokens and
 * refresh them as necessary.
 */
package rs.lukaj.android.stories.network;