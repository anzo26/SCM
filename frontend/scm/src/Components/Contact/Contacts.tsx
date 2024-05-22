"use client";
import React, {useState, useEffect} from 'react';
import {useRouter} from 'next/navigation';
import {Contact as ContactModel} from '../../models/Contact';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faTrash, faChevronLeft, faChevronRight, faInfo} from '@fortawesome/free-solid-svg-icons';
import {toast} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import {Tenant} from "@/models/Tenant";
import ContactExportPopup from "@/Components/Contact/ContactExportPopup";

interface ContactsProps {
    contacts: ContactModel[];
    tenantUniqueName: string;
    tenantId: string;
    IdToken: string;
    view: 'grid' | 'list';
    onDeleted: () => void;
    displayProps: string[];
    tenant: Tenant;
}

const Contacts: React.FC<ContactsProps> = ({
                                               contacts,
                                               tenantUniqueName,
                                               tenantId,
                                               IdToken,
                                               view,
                                               onDeleted,
                                               displayProps,
                                               tenant
                                           }) => {
    const router = useRouter();
    const [selectedContacts, setSelectedContacts] = useState<string[]>([]);
    const [selectAll, setSelectAll] = useState(false);
    const [showConfirmation, setShowConfirmation] = useState(false);
    const [contactToDelete, setContactToDelete] = useState<string | null>(null);
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [currentPage, setCurrentPage] = useState(1);
    const [contactsPerPage, setContactsPerPage] = useState(50);

    const handleViewDetails = (contactId: string, tenantUniqueName: string) => {
        router.push(`/contacts/${tenantUniqueName}/${contactId}`);
    };

    useEffect(() => {
        const savedViewMode = localStorage.getItem('viewMode');
        if (savedViewMode === 'list' || savedViewMode === 'grid') {
            setViewMode(savedViewMode as 'list' | 'grid');
        }
    }, [view]);

    const handleSelectContact = (contactId: string) => {
        setSelectedContacts(prevSelected => {
            if (prevSelected.includes(contactId)) {
                return prevSelected.filter(id => id !== contactId);
            } else {
                return [...prevSelected, contactId];
            }
        });
    };

    const handleSelectAll = () => {
        if (selectAll) {
            setSelectedContacts([]);
        } else {
            setSelectedContacts(contacts.map(contact => contact.id));
        }
        setSelectAll(!selectAll);
    };

    const handleDelete = async (contactId: string) => {
        setShowConfirmation(false);
        setContactToDelete(null);
        try {
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/contacts/${contactId}/${tenantUniqueName}`, {
                method: 'DELETE',
                headers: {
                    'userToken': `Bearer ${IdToken}`,
                },
            });

            if (!res.ok) {
                throw new Error(`Error deleting contact: ${res.statusText}`);
            }
            toast.success('Contact deleted successfully');
            onDeleted();
            router.refresh();
        } catch (error) {
            toast.error('Failed to delete contact');
            console.error('Failed to delete contact:', error);
        }
    };

    const confirmDelete = (contactId: string) => {
        setContactToDelete(contactId);
        setShowConfirmation(true);
    };

    const indexOfLastContact = currentPage * contactsPerPage;
    const indexOfFirstContact = indexOfLastContact - contactsPerPage;
    const currentContacts = contacts.slice(indexOfFirstContact, indexOfLastContact);

    const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

    return (
        <div>
            <div className="flex justify-between items-center mb-4">
                <div className="flex items-center">
                    <label htmlFor="contactsPerPage" className="mr-2">Show:</label>
                    <select
                        id="contactsPerPage"
                        value={contactsPerPage}
                        onChange={(e) => setContactsPerPage(Number(e.target.value))}
                        className="form-select"
                    >
                        <option value={10}>10</option>
                        <option value={25}>25</option>
                        <option value={50}>50</option>
                        <option value={100}>100</option>
                    </select>
                </div>
                <ContactExportPopup IdToken={IdToken} tenantUniqueName={tenantUniqueName} tenantId={tenantId}
                                    contactIds={selectedContacts}/>
            </div>
            {viewMode === 'grid' ? (
                <div>
                    <div className="flex items-center mb-3">
                        <input
                            type="checkbox"
                            checked={selectAll}
                            onChange={handleSelectAll}
                            className="form-checkbox h-5 w-5 text-primary-light transition duration-150 ease-in-out"
                        />
                        <span className="ml-2 text-xl">Select all</span>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-1">
                        {currentContacts.map(contact => (
                            <div
                                key={contact.id}
                                className="border p-4 rounded-lg shadow-md hover:shadow-xl transition-shadow duration-200 relative flex flex-col cursor-pointer"
                                onClick={() => handleViewDetails(contact.id, contact.tenantUniqueName)}
                            >
                                <div className="flex justify-between items-start mb-5">
                                    <input
                                        type="checkbox"
                                        checked={selectedContacts.includes(contact.id)}
                                        onClick={(e) => e.stopPropagation()}
                                        onChange={(e) => {
                                            handleSelectContact(contact.id);
                                        }}
                                        className="form-checkbox h-7 w-7 text-primary-light transition duration-150 ease-in-out"
                                    />
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            confirmDelete(contact.id);
                                        }}
                                        className="text-red-600 hover:text-red-800 transition">
                                        <FontAwesomeIcon className="w-5 h-5" icon={faTrash}/>
                                    </button>
                                </div>
                                <div className="text-center mb-6">
                                    {contact.props.name && displayProps.includes('name') && (
                                        <h2 className="text-xl font-bold mb-1.5">{contact.props.name}</h2>
                                    )}
                                    {contact.props.email && displayProps.includes('email') && (
                                        <p className="mb-1.5">{contact.props.email}</p>
                                    )}
                                    {contact.props.phoneNumber && displayProps.includes('phoneNumber') && (
                                        <p className="mb-1.5">{contact.props.phoneNumber}</p>
                                    )}
                                </div>

                                <div className="grid grid-cols-2 gap-x-4 gap-y-2 mb-6 text-center justify-center">
                                    {displayProps.filter(prop => prop !== 'name' && prop !== 'email' && prop !== 'phoneNumber').map(prop => (
                                        <div key={prop}>
                                            <strong>{tenant.labels[prop]}</strong>
                                            <p>{contact.props[prop]}</p>
                                        </div>
                                    ))}
                                </div>
                                <div className="flex justify-center flex-wrap gap-2">
                                    {contact.tags && contact.tags.map((tag, index) => (
                                        <span key={index}
                                              className="bg-white text-primary-light border border-primary-light text-sm font-medium px-3 py-1.5 rounded-8">
                            {tag}
                        </span>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>


            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white">
                        <thead>
                        <tr>
                            <th className="px-3 py-3 border-b-2 border-gray-300 text-left leading-4 text-gray-600 tracking-wider">
                                <div className="flex items-center w-5 h-5">
                                <input
                                        type="checkbox"
                                        checked={selectAll}
                                        onChange={handleSelectAll}
                                        className="form-checkbox h-5 w-5 text-primary-light transition duration-150 ease-in-out"
                                    />
                                </div>
                            </th>
                            {displayProps.map(prop => (
                                <th key={prop}
                                    className="px-3 py-3 border-b-2 border-gray-300 text-left leading-4 text-gray-600 tracking-wider">
                                    {tenant.labels[prop]}
                                </th>
                            ))}
                            <th className="px-3 py-3 border-b-2 border-gray-300 text-left leading-4 text-gray-600 tracking-wider"></th>
                        </tr>
                        </thead>
                        <tbody>
                        {currentContacts.map(contact => (
                            <tr key={contact.id}>
                                <td className="px-3 py-4 whitespace-no-wrap border-b border-gray-300">
                                    <input
                                        type="checkbox"
                                        checked={selectedContacts.includes(contact.id)}
                                        onChange={() => handleSelectContact(contact.id)}
                                        className="form-checkbox h-5 w-5 text-primary-light transition duration-150 ease-in-out"
                                    />
                                </td>
                                {displayProps.map(prop => (
                                    <td key={prop} className="px-3 py-4 whitespace-no-wrap border-b border-gray-300">
                                        <div className="text-sm leading-5 text-gray-800">{contact.props[prop]}</div>
                                    </td>
                                ))}
                                <td className="px-3 py-5 whitespace-no-wrap border-b border-gray-300 text-right flex items-center">
                                    <button
                                        onClick={() => handleViewDetails(contact.id, contact.tenantUniqueName)}
                                        className="text-primary-light hover:text-primary-dark transition relative group">
                                        <FontAwesomeIcon icon={faInfo} className="mr-2"/>
                                    </button>
                                    <button
                                        onClick={() => confirmDelete(contact.id)}
                                        className="text-red-600 hover:text-red-800 transition ml-4">
                                        <FontAwesomeIcon className="ml-1 w-3.5 h-auto" icon={faTrash}/>
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            <div className="flex justify-between items-center my-4">
                <button
                    onClick={() => paginate(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="px-4 py-2 rounded-8 bg-gray-300 text-black mr-2 disabled:opacity-50 disabled:cursor-not-allowed hover:scale-105 transition">
                    <FontAwesomeIcon className={"w-2.5 h-auto mr-1"} icon={faChevronLeft}/> Previous
                </button>
                <span
                    className="text-lg">{`Page ${currentPage} of ${Math.ceil(contacts.length / contactsPerPage)}`}</span>
                <button
                    onClick={() => paginate(currentPage + 1)}
                    disabled={currentPage === Math.ceil(contacts.length / contactsPerPage)}
                    className="px-4 py-2 rounded-8 bg-gray-300 text-black ml-2 disabled:opacity-50 disabled:cursor-not-allowed hover:scale-105 transition">
                    Next <FontAwesomeIcon className={"w-2.5 h-auto ml-1"} icon={faChevronRight}/>
                </button>
            </div>

            {showConfirmation && (
                <div className="fixed inset-0 flex items-center justify-center bg-gray-800 bg-opacity-75">
                    <div className="bg-white p-8 rounded-lg shadow-lg">
                        <h2 className="text-xl mb-4">Are you sure you want to delete this contact?</h2>
                        <div className="flex justify-end">
                            <button
                                onClick={() => setShowConfirmation(false)}
                                className="btn bg-gray-300 text-black mr-2">
                                Cancel
                            </button>
                            <button
                                onClick={() => handleDelete(contactToDelete!)}
                                className="btn bg-red-600 text-white">
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Contacts;
